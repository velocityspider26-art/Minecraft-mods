--!nonstrict
-- Animations -- keyframe data and the player that evaluates it.
--
-- These are authored animations. They are not procedural wobble: each one is a
-- timeline of poses with easing between them, the same thing you would build on
-- a track in Moon Animator, written as a table instead of clicked into a GUI.
-- The advantage of the table is that it is tunable in text -- "the mag seats
-- 150 ms too late" is a one-number fix -- and it needs no uploaded assets.
--
-- TRACK KINDS
--   weapon   CFrame, additive on the weapon root. The gun tilting toward you
--            during a reload.
--   camera   CFrame, tiny head/upper-body reaction. Never more than a few
--            centimetres/degrees; it sells weight without stealing aim.
--   left     hand target, weapon-local. Values may be:
--              "magwell"                        a named anchor in def.grips
--              CFrame.new(...)                  an explicit weapon-local pose
--              {grip = "magwell", offset = CF}  an anchor plus an offset
--              {grip = "left", follow = "pump"} an anchor that rides a bone,
--                                               so the support hand travels
--                                               with the pump for free
--   right    same, for the firing hand
--   mag      CFrame, offset applied to parts tagged bone = "mag"
--   bolt     NUMBER 0..1, fraction of def.boltTravel the bolt is rearward.
--   pump     NUMBER 0..1, same, for the shotgun's fore-end
--   magHide  step track of booleans -- the magazine is out of the world
--
-- Keyframes are {time, value, easing}. Easing names the curve used to arrive
-- AT that keyframe, so a "back" on the mag seating gives you the little
-- overshoot of a magazine being slapped home.
--
-- All timings are nominal. The player scales an animation to whatever duration
-- the caller asks for, so these stay in sync automatically when you retune
-- GameConfig.HANDLING.

local Animations = {}

local function deg(x: number, y: number, z: number): CFrame
	return CFrame.Angles(math.rad(x), math.rad(y), math.rad(z))
end

--------------------------------------------------------------------------------
-- Easing
--------------------------------------------------------------------------------
-- The curve is where "AAA" actually lives, and `quad` was the problem: it eases
-- BOTH ends, so every single keyframe was approached slowly and left slowly.
-- Stack forty of those and the result floats -- which is exactly the note
-- "doesn't feel like a battle-hardened operator". Trained hands do the
-- opposite: they leave instantly, travel fast, and stop dead.
--
-- Three curves below are the ones that do the heavy lifting:
--
--   anticipate  Dips BELOW the start pose before driving to the target. This is
--               the single oldest principle in character animation and the one
--               most missing here -- a person loads a movement before making
--               it, and its absence is most of why motion reads as a puppet
--               being dragged rather than a body deciding to move.
--   impact      Instant departure, dead stop, no deceleration. For anything
--               metal hitting metal.
--   settle      Overshoots and rings down over two decaying cycles. Real
--               follow-through, as opposed to `back`'s single bounce.
local EASE = {
	linear = function(t: number)
		return t
	end,
	quad = function(t: number)
		return t < 0.5 and 2 * t * t or 1 - (-2 * t + 2) ^ 2 / 2
	end,
	out = function(t: number) -- fast departure, soft arrival: hand travel
		return 1 - (1 - t) ^ 3
	end,
	sharp = function(t: number)
		return t * t * t
	end,
	snap = function(t: number) -- almost instant: a bolt dropping
		return 1 - (1 - t) ^ 5
	end,
	back = function(t: number) -- overshoots then settles: seating a magazine
		local c1 = 1.9
		local c3 = c1 + 1
		return 1 + c3 * (t - 1) ^ 3 + c1 * (t - 1) ^ 2
	end,

	-- Loads the movement: dips to about -8% before accelerating hard.
	anticipate = function(t: number)
		return t * t * (2.7 * t - 1.7)
	end,
	-- Harder than snap. Bolt home, mag seated, fore-end bottoming out.
	impact = function(t: number)
		return 1 - (1 - t) ^ 7
	end,
	-- Firm human contact: decisive, but not the near-teleport produced by impact.
	-- Used for hands and the rifle body around mag seating and bolt manipulation.
	contact = function(t: number)
		return 1 - (1 - t) ^ 3
	end,
	-- Overshoot plus a decaying second bounce -- follow-through, not a bounce.
	settle = function(t: number)
		return 1 - math.exp(-5.5 * t) * math.cos(7.5 * t)
	end,
	-- Weight transfer: slow to break, then runs away. For the weapon coming
	-- off the shoulder, which a rifle does reluctantly and then all at once.
	heavy = function(t: number)
		return t * t * t * (t * (6 * t - 15) + 10) -- smootherstep
	end,
}

-- Clone an animation before changing one track. Variants share most of their
-- mechanical beats, but they must not share mutable keyframe tables.
local function cloneAnimation(base: any): any
	local out = {}
	for key, value in base do
		if type(value) == "table" then
			local copied = {}
			for i, row in value do
				if type(row) == "table" then
					local rowCopy = {}
					for k, cell in row do
						rowCopy[k] = cell
					end
					copied[i] = rowCopy
				else
					copied[i] = row
				end
			end
			out[key] = copied
		else
			out[key] = value
		end
	end
	return out
end

--------------------------------------------------------------------------------
-- CARBINE SET -- magazine-fed rifle handling for the MK18.
--
-- The pistol has its own set below. Reusing these shoulder-driven poses on a
-- sidearm was one of the main reasons the old motion looked like a scaled-down
-- rifle instead of a compact two-handed handgun presentation.
--------------------------------------------------------------------------------
-- The reload tilt was far too polite: 19 degrees of yaw and 15 cm of drop, so
-- the weapon barely acknowledged that a magazine was being changed. A real
-- reload brings the gun down and IN, rotates the magwell toward your support
-- hand so you can see it, and the whole thing happens closer to your chest.
-- 34 degrees of yaw, 22 of roll, and it drops nearly a third of a stud.
-- THE RELOAD HAS TO HAPPEN IN FRAME.
--
-- The drop was 0.30 studs and it put the whole action below the bottom of the
-- screen -- you had to look down to see your own reload, which is useless. A
-- real reload does not drop the weapon much at all: it brings it IN toward the
-- chest and ROTATES the magwell toward the support hand so you can see what you
-- are doing. So the vertical travel is a third of what it was and the rotation
-- does the work instead: 40 degrees of yaw, 26 of roll, and it comes back
-- toward the camera in Z rather than going down in Y.
local TILT = CFrame.new(0.015, 0.055, -0.025) * deg(6, 1.5, -9)

-- Rolled further over while the fresh magazine is driven home, because that is
-- the moment you are actually looking into the well.  Positive Y is UP in
-- camera space; the previous negative-Y values pushed the magwell toward the
-- bottom bezel even after the low-ready bug was removed.
local TILT_SEAT = CFrame.new(0.010, 0.065, -0.030) * deg(8, 1.0, -11)

local MAGFED = {}

-- A tactical reload, re-timed as a sequence of DECISIONS rather than a smooth
-- sweep. Read down the weapon track: load (anticipate), break the gun off the
-- shoulder, HOLD dead still while the hands work, drive it back, ring down.
-- The holds matter as much as the moves -- constant motion reads as floating,
-- and stillness at the extremes is what reads as control.
MAGFED.reload_tactical = {
	length = 1.62,
	technique = "pouch_stow",
	cycleOrder = 2,
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.20, CFrame.new(-0.012, -0.018, 0.012) * deg(0.45, -0.35, 0.55), "out" },
		{ 0.94, CFrame.new(-0.012, -0.018, 0.012) * deg(0.45, -0.35, 0.55), "linear" },
		{ 1.42, CFrame.new(0.006, 0.010, -0.018) * deg(-0.55, 0.20, -0.30), "impact" },
		{ 1.62, CFrame.identity, "settle" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		-- 60 ms of anticipation: the muzzle rises a touch as the gun is
		-- unloaded off the shoulder, before it drops. Nobody notices it and
		-- everybody notices its absence.
		{ 0.06, CFrame.new(-0.01, 0.03, -0.02) * deg(-3, -4, 3), "out" },
		{ 0.20, TILT, "heavy" }, -- reluctant, then all at once
		{ 0.40, TILT_SEAT, "out" }, -- rolls the well over to look at it
		{ 0.94, TILT_SEAT, "linear" }, -- HOLD. The hands are working.
		{ 1.20, CFrame.new(0.06, -0.16, 0.11) * deg(6, 18, -12), "out" },
		-- Thrown back into the shoulder, not glided. `settle` rings it down.
		{ 1.42, CFrame.identity, "impact" },
		{ 1.62, CFrame.identity, "settle" },
	},
	-- Timed to the magazine keyframe for keyframe, so the hand and the thing it
	-- is carrying arrive together.
	left = {
		{ 0.00, "left" },
		{ 0.05, { grip = "left", offset = CFrame.new(0, 0.03, -0.04) }, "out" }, -- load
		{ 0.16, "magwell", "impact" }, -- hand SLAPS onto the mag
		{ 0.22, "magwell", "linear" }, -- brief hold: fingers close
		{ 0.62, "pouch", "out" }, -- carries it down to the rig
		{ 0.80, "pouch", "linear" }, -- HOLD: stow, take a fresh one
		{ 1.06, { grip = "magwell", offset = CFrame.new(-0.14, -0.62, 0.26) }, "out" },
		{ 1.24, { grip = "magwell", offset = CFrame.new(0, -0.20, 0.05) }, "linear" },
		{ 1.34, "magwell", "impact" }, -- drives it home
		{ 1.40, "magwell", "linear" },
		{ 1.62, "left", "out" }, -- back onto the handguard
	},
	-- The firing hand does not sit still. It rides the grip, but the wrist
	-- rolls with the weapon and takes the recovery jolt a frame late -- which is
	-- the cheapest secondary motion available and it sells the whole thing.
	right = {
		{ 0.00, "right" },
		{ 0.24, { grip = "right", offset = CFrame.new(0, -0.012, 0.008) }, "out" },
		{ 1.20, { grip = "right", offset = CFrame.new(0, -0.012, 0.008) }, "linear" },
		{ 1.46, { grip = "right", offset = CFrame.new(0, 0.010, -0.010) }, "impact" },
		{ 1.62, "right", "settle" },
	},
	-- THE MAGAZINE HAS TO TRAVEL WITH THE HAND.
	--
	-- This is why the reload still read as fake. The old mag track dropped
	-- straight down 0.9 studs and sat there, while the support hand went to
	-- `pouch` -- down AND out to the left. They diverged completely: you watched
	-- a magazine hover in mid-air below the weapon, a hand go somewhere else
	-- entirely, and then a magazine appear back in the well. Nothing was ever
	-- being carried by anything.
	--
	-- POUCH_DELTA is the offset that puts the magazine exactly at the pouch grip
	-- anchor: pouch (-0.34, -1.42, 0.18) minus the mag's rest position
	-- (0, -0.20, -0.34). Now the hand strips it, carries it down to the rig,
	-- stows it, and brings a fresh one back up the same path.
	mag = {
		{ 0.00, CFrame.identity },
		{ 0.18, CFrame.identity, "linear" }, -- still seated; the hand is arriving
		-- A magazine does not fall out, it is ROCKED out: the front lip clears
		-- first, so it pitches forward before it drops.
		{ 0.24, CFrame.new(0, -0.07, 0.02) * deg(-9, 0, 0), "out" },
		{ 0.34, CFrame.new(-0.05, -0.48, 0.13) * deg(-20, 0, 6), "sharp" },
		-- Carried down to the rig, in the hand, along the hand's own path.
		{ 0.62, CFrame.new(-0.34, -1.22, 0.52) * deg(-38, 14, 18), "out" },
		{ 0.80, CFrame.new(-0.34, -1.22, 0.52) * deg(-38, 14, 18), "linear" },
		-- Fresh one comes back UP from the rig on the same line.
		{ 1.06, CFrame.new(-0.14, -0.62, 0.26) * deg(-24, 8, 10), "out" },
		{ 1.24, CFrame.new(0, -0.20, 0.05) * deg(-9, 0, 0), "linear" },
		{ 1.34, CFrame.identity, "impact" }, -- slapped home, dead stop
	},
	-- Hidden only for the 160 ms the swap actually takes, at the rig, off to the
	-- side where it is half out of frame anyway. Previously the magazine was
	-- invisible for over half the reload.
	magHide = { { 0.74, true }, { 0.88, false } },
}

-- VARIANT B: the "beer can" grip. Instead of stripping the old magazine and
-- carrying it down, the support hand comes up holding the FRESH mag already,
-- indexes it against the old one, rocks the old one out and drives the new one
-- in on the same motion. It is a real technique, it is measurably faster, and
-- crucially it looks completely different from variant A -- which is the whole
-- point of having variants at all. Note the weapon barely rolls: you are not
-- looking into the well because your hand already knows where it is.
MAGFED.reload_tactical_alt_unavailable = {
	length = 1.48,
	technique = "l_index",
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.18, CFrame.new(-0.008, -0.012, 0.008) * deg(0.30, -0.25, 0.35), "out" },
		{ 0.86, CFrame.new(-0.008, -0.012, 0.008) * deg(0.30, -0.25, 0.35), "linear" },
		{ 1.30, CFrame.new(0.004, 0.006, -0.014) * deg(-0.40, 0.15, -0.22), "impact" },
		{ 1.48, CFrame.identity, "settle" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		{ 0.05, CFrame.new(-0.01, 0.02, -0.02) * deg(-2, -3, 2), "out" },
		{ 0.18, CFrame.new(0.09, -0.07, 0.17) * deg(9, 26, -17), "heavy" },
		{ 0.86, CFrame.new(0.09, -0.07, 0.17) * deg(9, 26, -17), "linear" },
		{ 1.14, CFrame.new(0.04, -0.03, 0.07) * deg(4, 11, -7), "out" },
		{ 1.30, CFrame.identity, "impact" },
		{ 1.48, CFrame.identity, "settle" },
	},
	left = {
		-- Straight to the rig FIRST: the fresh magazine is in hand before the
		-- old one is touched.
		{ 0.00, "left" },
		{ 0.20, "pouch", "out" },
		{ 0.34, "pouch", "linear" },
		-- Comes up to the well holding it, indexes against the old mag.
		{ 0.62, { grip = "magwell", offset = CFrame.new(-0.10, -0.34, 0.16) }, "out" },
		{ 0.74, { grip = "magwell", offset = CFrame.new(-0.06, -0.16, 0.09) }, "linear" },
		{ 0.86, { grip = "magwell", offset = CFrame.new(-0.05, -0.13, 0.07) }, "linear" },
		{ 1.06, "magwell", "impact" }, -- new one driven straight in
		{ 1.14, "magwell", "linear" },
		{ 1.48, "left", "out" },
	},
	right = {
		{ 0.00, "right" },
		{ 0.22, { grip = "right", offset = CFrame.new(0, -0.010, 0.006) }, "out" },
		{ 1.14, { grip = "right", offset = CFrame.new(0, -0.010, 0.006) }, "linear" },
		{ 1.34, { grip = "right", offset = CFrame.new(0, 0.009, -0.009) }, "impact" },
		{ 1.48, "right", "settle" },
	},
	-- The old mag is rocked out late and drops clear, because the hand is busy
	-- holding the new one -- so this variant does NOT retain it neatly, it just
	-- gets it out of the way.
	mag = {
		{ 0.00, CFrame.identity },
		{ 0.80, CFrame.identity, "linear" },
		{ 0.88, CFrame.new(0, -0.09, 0.03) * deg(-11, 0, 0), "out" },
		{ 0.98, CFrame.new(0.03, -0.86, 0.14) * deg(-34, 5, 8), "sharp" },
		{ 1.02, CFrame.new(-0.05, -0.20, 0.05) * deg(-9, 0, 0), "linear" }, -- fresh
		{ 1.06, CFrame.identity, "impact" },
	},
	magHide = { { 0.99, true }, { 1.02, false } },
}

MAGFED.reload_empty = {
	length = 1.56,
	technique = "speed_drop",
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.16, CFrame.new(-0.010, -0.016, 0.012) * deg(0.50, -0.30, 0.45), "sharp" },
		{ 0.86, CFrame.new(-0.010, -0.016, 0.012) * deg(0.50, -0.30, 0.45), "linear" },
		{ 1.34, CFrame.new(0.008, 0.014, -0.024) * deg(-0.75, 0.25, -0.38), "impact" },
		{ 1.56, CFrame.identity, "settle" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		-- No anticipation here. A speed reload starts on a dry click and the
		-- gun comes off the shoulder NOW -- the absence of the load-up is the
		-- difference between "changing a magazine" and "fixing a problem".
		{ 0.16, TILT, "sharp" },
		{ 0.34, TILT_SEAT, "out" },
		{ 0.86, TILT_SEAT, "linear" }, -- HOLD
		{ 1.10, CFrame.new(0.06, -0.15, 0.10) * deg(6, 17, -12), "out" },
		-- The bolt going home is a real jolt: 3 kg of carrier slamming forward
		-- and the whole gun moves with it. Dead stop, then ring down.
		{ 1.34, CFrame.new(0.02, -0.05, 0.15) * deg(-5, 9, -7), "impact" },
		{ 1.56, CFrame.identity, "settle" },
	},
	-- Emergency: the hand does NOT carry the empty magazine anywhere. It hits the
	-- release, the mag drops free, and the hand goes straight to the rig for a
	-- fresh one -- which is the entire reason this reload is faster.
	left = {
		{ 0.00, "left" },
		{ 0.12, "release", "impact" }, -- hits the release, hard
		{ 0.20, "release", "linear" },
		{ 0.42, "pouch", "out" }, -- straight to the rig, no detour
		{ 0.62, "pouch", "linear" },
		{ 0.90, { grip = "magwell", offset = CFrame.new(-0.14, -0.62, 0.26) }, "out" },
		{ 1.04, { grip = "magwell", offset = CFrame.new(0, -0.20, 0.05) }, "linear" },
		{ 1.14, "magwell", "impact" },
		{ 1.26, "release", "out" }, -- slaps the bolt release
		{ 1.34, "release", "linear" },
		{ 1.56, "left", "out" },
	},
	right = {
		{ 0.00, "right" },
		{ 0.20, { grip = "right", offset = CFrame.new(0, -0.014, 0.010) }, "out" },
		{ 1.10, { grip = "right", offset = CFrame.new(0, -0.014, 0.010) }, "linear" },
		-- Takes the bolt-home jolt through the wrist, a frame after the weapon.
		{ 1.38, { grip = "right", offset = CFrame.new(0, 0.012, -0.012) }, "impact" },
		{ 1.56, "right", "settle" },
	},
	-- The empty one FALLS, under something like gravity, and keeps going until
	-- it is out of frame. It is not carried and it is not retained -- that is
	-- the trade you took, and you should be able to see it leave.
	mag = {
		{ 0.00, CFrame.identity },
		{ 0.12, CFrame.identity, "linear" },
		{ 0.18, CFrame.new(0, -0.12, 0.03) * deg(-12, 0, 0), "out" },
		-- `sharp` is t^3, which accelerates -- so this genuinely reads as
		-- falling rather than as being lowered.
		{ 0.32, CFrame.new(0.02, -0.72, 0.11) * deg(-30, 4, 6), "sharp" },
		{ 0.48, CFrame.new(0.05, -1.80, 0.22) * deg(-52, 10, 14), "sharp" },
		-- Fresh one arrives from the rig, on the other side, so the two never
		-- look like the same magazine bouncing.
		{ 0.90, CFrame.new(-0.14, -0.62, 0.26) * deg(-24, 8, 10), "out" },
		{ 1.04, CFrame.new(0, -0.22, 0.05) * deg(-9, 0, 0), "linear" },
		{ 1.14, CFrame.identity, "impact" },
	},
	-- Hidden from the moment it clears the bottom of frame until the fresh one
	-- is in the hand.
	magHide = { { 0.50, true }, { 0.90, false } },
	-- The bolt is held back for the whole reload and only drops at the slap.
	bolt = {
		{ 0.00, 1 },
		{ 1.28, 1, "linear" },
		{ 1.33, 0, "impact" },
	},
}


--------------------------------------------------------------------------------
-- MK18 RELOAD VARIATIONS
--
-- Every variation keeps the magazine, hand and weapon on the same path. The
-- low-probability fumbles are not comedy animations: the mag catches the front
-- lip, the hand corrects by a few centimetres, and the second drive succeeds.
--------------------------------------------------------------------------------

-- Fast emergency reload: the old magazine is allowed to fall while the fresh
-- one is already moving upward. This is intentionally separate from tactical
-- retention so the visuals agree with the gameplay state.
MAGFED.reload_emergency = cloneAnimation(MAGFED.reload_tactical_alt_unavailable)
MAGFED.reload_emergency.chanceWeight = 1.0
MAGFED.reload_emergency.weapon = {
	{ 0.00, CFrame.identity },
	{ 0.12, CFrame.new(0.10, -0.03, 0.20) * deg(8, 30, -20), "sharp" },
	{ 0.68, CFrame.new(0.12, -0.04, 0.23) * deg(10, 34, -23), "linear" },
	{ 1.12, CFrame.new(0.03, -0.01, 0.06) * deg(3, 8, -5), "out" },
	{ 1.30, CFrame.identity, "impact" },
	{ 1.48, CFrame.identity, "settle" },
}

-- High-centre workspace: the receiver stays closer to the eye and the support
-- hand works vertically rather than sweeping far toward the hip.
MAGFED.reload_tactical_b = cloneAnimation(MAGFED.reload_tactical_alt_unavailable)
MAGFED.reload_tactical_b.technique = "l_index_retention"
MAGFED.reload_tactical_b.cycleOrder = 1
MAGFED.reload_tactical_b.chanceWeight = 1.0
-- Two physical magazine models are visible at once. The fresh magazine comes
-- out of the pouch first, is indexed beside the seated magazine, the old one is
-- pulled into the same hand, and the fresh one is driven home. This is the
-- tactical exchange the previous single-mag rig could not represent.
MAGFED.reload_tactical_b.weapon = {
	{ 0.00, CFrame.identity },
	{ 0.08, CFrame.new(-0.02, 0.04, -0.02) * deg(-3, -4, 3), "out" },
	{ 0.22, CFrame.new(-0.14, 0.16, 0.20) * deg(8, 28, -18), "heavy" },
	{ 0.58, CFrame.new(-0.18, 0.20, 0.25) * deg(12, 35, -23), "out" },
	{ 1.08, CFrame.new(-0.18, 0.20, 0.25) * deg(12, 35, -23), "linear" },
	{ 1.30, CFrame.new(-0.06, 0.07, 0.09) * deg(4, 12, -8), "out" },
	{ 1.46, CFrame.identity, "impact" },
	{ 1.62, CFrame.identity, "settle" },
}
MAGFED.reload_tactical_b.left = {
	{ 0.00, "left" },
	{ 0.16, "pouch", "sharp" },
	{ 0.34, "pouch", "linear" }, -- fresh magazine acquired
	{ 0.60, { grip = "magwell", offset = CFrame.new(-0.14, -0.34, 0.18) * deg(0, 0, 9) }, "out" },
	{ 0.76, { grip = "magwell", offset = CFrame.new(-0.08, -0.17, 0.10) * deg(0, 0, 9) }, "linear" },
	{ 0.90, { grip = "magwell", offset = CFrame.new(-0.06, -0.13, 0.08) * deg(0, 0, 10) }, "impact" },
	{ 1.12, "magwell", "impact" }, -- fresh magazine seats
	{ 1.24, { grip = "magwell", offset = CFrame.new(0, -0.07, 0.02) }, "out" },
	{ 1.32, "magwell", "impact" }, -- positive baseplate check
	{ 1.62, "left", "out" },
}
MAGFED.reload_tactical_b.right = {
	{ 0.00, "right" },
	{ 0.24, { grip = "right", offset = CFrame.new(0, -0.012, 0.008) }, "out" },
	{ 1.30, { grip = "right", offset = CFrame.new(0, -0.012, 0.008) }, "linear" },
	{ 1.48, { grip = "right", offset = CFrame.new(0, 0.010, -0.010) }, "impact" },
	{ 1.62, "right", "settle" },
}
-- OLD magazine: remains seated while the fresh one approaches, then is rocked
-- out and retained against the hand before travelling back toward the rig.
MAGFED.reload_tactical_b.mag = {
	{ 0.00, CFrame.identity },
	{ 0.72, CFrame.identity, "linear" },
	{ 0.82, CFrame.new(0, -0.10, 0.03) * deg(-10, 0, 0), "out" },
	{ 0.94, CFrame.new(0.10, -0.24, 0.10) * deg(-18, -4, -13), "impact" },
	{ 1.10, CFrame.new(0.16, -0.42, 0.20) * deg(-25, -8, -18), "out" },
	{ 1.28, CFrame.new(-0.24, -1.08, 0.48) * deg(-38, 12, 18), "out" },
}
-- FRESH magazine: a duplicate of the weapon magazine, visible from the pouch
-- all the way to the magwell. At seating time the duplicate and main magazine
-- swap invisibility while occupying the same pose, so there is no pop.
MAGFED.reload_tactical_b.spareMag = {
	{ 0.00, CFrame.new(-0.34, -1.22, 0.52) * deg(-38, 14, 18) },
	{ 0.34, CFrame.new(-0.34, -1.22, 0.52) * deg(-38, 14, 18), "linear" },
	{ 0.60, CFrame.new(-0.14, -0.62, 0.26) * deg(-24, 8, 10), "out" },
	{ 0.78, CFrame.new(-0.07, -0.25, 0.10) * deg(-11, 0, 8), "linear" },
	{ 0.96, CFrame.new(-0.03, -0.16, 0.06) * deg(-7, 0, 4), "out" },
	{ 1.12, CFrame.identity, "impact" },
}
MAGFED.reload_tactical_b.magHide = { { 1.10, true }, { 1.13, false } }
MAGFED.reload_tactical_b.spareMagHide = { { 0.00, true }, { 0.15, false }, { 1.13, true } }


-- Front-lip miss. The first upward drive hits the edge of the well, the wrist
-- backs off, changes angle, and seats it on the second attempt.
MAGFED.reload_tactical_c = cloneAnimation(MAGFED.reload_tactical)
MAGFED.reload_tactical_c.durationScale = 1.18
MAGFED.reload_tactical_c.technique = "magwell_fumble"
MAGFED.reload_tactical_c.chanceWeight = 0.55
MAGFED.reload_tactical_c.fumbleAt = 0.73
MAGFED.reload_tactical_c.weapon = {
	{ 0.00, CFrame.identity },
	{ 0.18, TILT, "heavy" },
	{ 0.38, TILT_SEAT, "out" },
	{ 1.20, TILT_SEAT, "linear" },
	{ 1.34, CFrame.new(0.18, -0.09, 0.30) * deg(18, 50, -33), "impact" },
	{ 1.48, CFrame.new(0.06, -0.03, 0.12) * deg(6, 17, -11), "out" },
	{ 1.62, CFrame.identity, "settle" },
}
MAGFED.reload_tactical_c.left = {
	{ 0.00, "left" },
	{ 0.16, "magwell", "impact" },
	{ 0.56, "pouch", "out" },
	{ 0.76, "pouch", "linear" },
	{ 1.04, { grip = "magwell", offset = CFrame.new(-0.12, -0.54, 0.25) }, "out" },
	{ 1.18, { grip = "magwell", offset = CFrame.new(0.07, -0.08, 0.06) * deg(-3, 0, 8) }, "impact" },
	{ 1.28, { grip = "magwell", offset = CFrame.new(-0.04, -0.24, 0.08) * deg(-8, 0, -4) }, "out" },
	{ 1.42, "magwell", "impact" },
	{ 1.62, "left", "out" },
}
MAGFED.reload_tactical_c.mag = {
	{ 0.00, CFrame.identity },
	{ 0.22, CFrame.new(0, -0.08, 0.02) * deg(-9, 0, 0), "out" },
	{ 0.56, CFrame.new(-0.34, -1.22, 0.52) * deg(-38, 14, 18), "out" },
	{ 0.76, CFrame.new(-0.34, -1.22, 0.52) * deg(-38, 14, 18), "linear" },
	{ 1.04, CFrame.new(-0.12, -0.54, 0.25) * deg(-22, 8, 10), "out" },
	{ 1.18, CFrame.new(0.07, -0.08, 0.06) * deg(-3, 0, 8), "impact" },
	{ 1.28, CFrame.new(-0.04, -0.24, 0.08) * deg(-8, 0, -4), "out" },
	{ 1.42, CFrame.identity, "impact" },
}
MAGFED.reload_tactical_c.magHide = { { 0.70, true }, { 0.86, false } }

-- Pouch snag. The support hand arrives, catches for a fraction, then rips the
-- magazine free. The gun stays stable; only the support side loses rhythm.
MAGFED.reload_tactical_d = cloneAnimation(MAGFED.reload_tactical)
MAGFED.reload_tactical_d.durationScale = 1.11
MAGFED.reload_tactical_d.technique = "pouch_snag"
MAGFED.reload_tactical_d.chanceWeight = 0.70
MAGFED.reload_tactical_d.left = {
	{ 0.00, "left" },
	{ 0.16, "magwell", "impact" },
	{ 0.58, "pouch", "out" },
	{ 0.82, "pouch", "linear" },
	{ 0.90, { grip = "pouch", offset = CFrame.new(-0.07, -0.05, 0.04) * deg(0, 0, -7) }, "impact" },
	{ 1.04, { grip = "magwell", offset = CFrame.new(-0.15, -0.62, 0.27) }, "sharp" },
	{ 1.26, { grip = "magwell", offset = CFrame.new(0, -0.19, 0.05) }, "linear" },
	{ 1.38, "magwell", "impact" },
	{ 1.62, "left", "out" },
}
MAGFED.reload_tactical_d.mag = {
	{ 0.00, CFrame.identity },
	{ 0.24, CFrame.new(0, -0.08, 0.02) * deg(-9, 0, 0), "out" },
	{ 0.58, CFrame.new(-0.34, -1.22, 0.52) * deg(-38, 14, 18), "out" },
	{ 0.90, CFrame.new(-0.41, -1.27, 0.56) * deg(-40, 13, 11), "impact" },
	{ 1.04, CFrame.new(-0.15, -0.62, 0.27) * deg(-24, 8, 10), "sharp" },
	{ 1.26, CFrame.new(0, -0.19, 0.05) * deg(-8, 0, 0), "linear" },
	{ 1.38, CFrame.identity, "impact" },
}
MAGFED.reload_tactical_d.magHide = { { 0.72, true }, { 0.94, false } }

-- Positive seating check: the magazine seats normally, then the palm gives the
-- baseplate a quick confirming strike before returning to the handguard.
MAGFED.reload_tactical_e = cloneAnimation(MAGFED.reload_tactical)
MAGFED.reload_tactical_e.durationScale = 1.06
MAGFED.reload_tactical_e.technique = "seat_check"
MAGFED.reload_tactical_e.cycleOrder = 5
MAGFED.reload_tactical_e.left = {
	{ 0.00, "left" },
	{ 0.16, "magwell", "impact" },
	{ 0.62, "pouch", "out" },
	{ 0.80, "pouch", "linear" },
	{ 1.08, { grip = "magwell", offset = CFrame.new(-0.12, -0.56, 0.24) }, "out" },
	{ 1.28, "magwell", "impact" },
	{ 1.38, { grip = "magwell", offset = CFrame.new(0, -0.08, 0.03) }, "out" },
	{ 1.46, "magwell", "impact" },
	{ 1.62, "left", "out" },
}
MAGFED.reload_tactical_e.weapon = {
	{ 0.00, CFrame.identity },
	{ 0.20, TILT, "heavy" },
	{ 0.40, TILT_SEAT, "out" },
	{ 1.26, TILT_SEAT, "linear" },
	{ 1.46, CFrame.new(0.11, -0.07, 0.23) * deg(12, 35, -23), "impact" },
	{ 1.62, CFrame.identity, "settle" },
}

-- Cross-body retention. The old magazine is held close to the chest rather than
-- dropped all the way to the belt line, making the whole silhouette different.
MAGFED.reload_tactical_f = cloneAnimation(MAGFED.reload_tactical)
MAGFED.reload_tactical_f.technique = "cross_body"
MAGFED.reload_tactical_f.cycleOrder = 3
MAGFED.reload_tactical_f.left = {
	{ 0.00, "left" },
	{ 0.15, "magwell", "impact" },
	{ 0.38, { grip = "magwell", offset = CFrame.new(-0.26, -0.46, 0.30) * deg(-12, 12, 20) }, "out" },
	{ 0.70, { grip = "pouch", offset = CFrame.new(0.15, 0.32, -0.10) }, "out" },
	{ 0.84, { grip = "pouch", offset = CFrame.new(0.15, 0.32, -0.10) }, "linear" },
	{ 1.12, { grip = "magwell", offset = CFrame.new(-0.10, -0.48, 0.22) }, "out" },
	{ 1.32, "magwell", "impact" },
	{ 1.62, "left", "out" },
}
MAGFED.reload_tactical_f.mag = {
	{ 0.00, CFrame.identity },
	{ 0.20, CFrame.new(0, -0.08, 0.02) * deg(-9, 0, 0), "out" },
	{ 0.38, CFrame.new(-0.26, -0.46, 0.30) * deg(-12, 12, 20), "out" },
	{ 0.70, CFrame.new(-0.19, -0.90, 0.42) * deg(-28, 18, 22), "out" },
	{ 0.84, CFrame.new(-0.19, -0.90, 0.42) * deg(-28, 18, 22), "linear" },
	{ 1.12, CFrame.new(-0.10, -0.48, 0.22) * deg(-20, 7, 9), "out" },
	{ 1.32, CFrame.identity, "impact" },
}
MAGFED.reload_tactical_f.magHide = { { 0.78, true }, { 0.92, false } }

-- Clean speed retention. Lower probability because it is notably faster and
-- should read as the occasional perfect repetition, not the default every time.
MAGFED.reload_tactical_g = cloneAnimation(MAGFED.reload_tactical_b)
MAGFED.reload_tactical_g.technique = "fast_l_index"
MAGFED.reload_tactical_g.cycleOrder = 6
MAGFED.reload_tactical_g.durationScale = 0.94
MAGFED.reload_tactical_g.chanceWeight = 0.65
MAGFED.reload_tactical_g.weapon = {
	{ 0.00, CFrame.identity },
	{ 0.16, CFrame.new(0.06, -0.02, 0.28) * deg(13, 29, -19), "sharp" },
	{ 1.12, CFrame.new(0.06, -0.02, 0.28) * deg(13, 29, -19), "linear" },
	{ 1.38, CFrame.identity, "impact" },
	{ 1.62, CFrame.identity, "settle" },
}

-- Muzzle-up chest workspace. This is deliberately authored from scratch rather
-- than cloned from the normal tilt: the rifle comes almost vertical, the old
-- magazine is retained high against the vest, and the fresh magazine travels a
-- short vertical line. Its silhouette cannot be mistaken for pouch-stow or the
-- L-index exchange even when viewed peripherally.
MAGFED.reload_tactical_h = {
	length = 1.72,
	technique = "muzzle_up_workspace",
	cycleOrder = 4,
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.20, CFrame.new(0.006, -0.012, 0.010) * deg(0.35, 0.20, -0.35), "out" },
		{ 1.30, CFrame.new(0.006, -0.012, 0.010) * deg(0.35, 0.20, -0.35), "linear" },
		{ 1.52, CFrame.new(-0.004, 0.008, -0.014) * deg(-0.45, -0.15, 0.25), "impact" },
		{ 1.72, CFrame.identity, "settle" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		{ 0.08, CFrame.new(0, 0.03, -0.03) * deg(-4, 2, -3), "out" },
		{ 0.24, CFrame.new(-0.10, 0.25, -0.02) * deg(-20, 17, -34), "heavy" },
		{ 1.28, CFrame.new(-0.10, 0.25, -0.02) * deg(-20, 17, -34), "linear" },
		{ 1.50, CFrame.new(-0.03, 0.08, -0.04) * deg(-7, 6, -11), "out" },
		{ 1.62, CFrame.identity, "impact" },
		{ 1.72, CFrame.identity, "settle" },
	},
	left = {
		{ 0.00, "left" },
		{ 0.18, "magwell", "impact" },
		{ 0.34, { grip = "magwell", offset = CFrame.new(-0.18, -0.32, 0.18) * deg(-8, 8, 16) }, "out" },
		{ 0.58, { grip = "pouch", offset = CFrame.new(0.16, 0.42, -0.12) }, "out" },
		{ 0.78, { grip = "pouch", offset = CFrame.new(0.16, 0.42, -0.12) }, "linear" },
		{ 1.06, { grip = "magwell", offset = CFrame.new(-0.08, -0.42, 0.18) }, "out" },
		{ 1.28, "magwell", "impact" },
		{ 1.40, { grip = "magwell", offset = CFrame.new(0, -0.07, 0.02) }, "out" },
		{ 1.48, "magwell", "impact" },
		{ 1.72, "left", "out" },
	},
	right = {
		{ 0.00, "right" },
		{ 0.28, { grip = "right", offset = CFrame.new(0.01, -0.015, 0.008) * deg(0, 0, -4) }, "out" },
		{ 1.42, { grip = "right", offset = CFrame.new(0.01, -0.015, 0.008) * deg(0, 0, -4) }, "linear" },
		{ 1.62, "right", "impact" },
	},
	mag = {
		{ 0.00, CFrame.identity },
		{ 0.20, CFrame.identity, "linear" },
		{ 0.34, CFrame.new(-0.18, -0.32, 0.18) * deg(-16, 10, 18), "out" },
		{ 0.58, CFrame.new(-0.18, -0.82, 0.38) * deg(-30, 16, 22), "out" },
		{ 0.78, CFrame.new(-0.18, -0.82, 0.38) * deg(-30, 16, 22), "linear" },
		{ 1.06, CFrame.new(-0.08, -0.42, 0.18) * deg(-18, 7, 10), "out" },
		{ 1.28, CFrame.identity, "impact" },
	},
	magHide = { { 0.70, true }, { 0.88, false } },
}

-- Emergency variations -------------------------------------------------------
MAGFED.reload_emergency_b = cloneAnimation(MAGFED.reload_emergency)
MAGFED.reload_emergency_b.durationScale = 1.10
MAGFED.reload_emergency_b.chanceWeight = 0.70
MAGFED.reload_emergency_b.fumbleAt = 0.70
MAGFED.reload_emergency_b.left = {
	{ 0.00, "left" },
	{ 0.10, "release", "impact" },
	{ 0.30, "pouch", "sharp" },
	{ 0.70, { grip = "magwell", offset = CFrame.new(-0.10, -0.34, 0.15) }, "out" },
	{ 0.94, { grip = "magwell", offset = CFrame.new(0.06, -0.08, 0.05) * deg(-2, 0, 7) }, "impact" },
	{ 1.06, { grip = "magwell", offset = CFrame.new(-0.03, -0.18, 0.06) }, "out" },
	{ 1.18, "magwell", "impact" },
	{ 1.48, "left", "out" },
}
MAGFED.reload_emergency_b.mag = {
	{ 0.00, CFrame.identity },
	{ 0.12, CFrame.new(0, -0.12, 0.03) * deg(-12, 0, 0), "out" },
	{ 0.30, CFrame.new(0.04, -1.45, 0.18) * deg(-48, 8, 12), "sharp" },
	{ 0.70, CFrame.new(-0.10, -0.34, 0.15) * deg(-18, 7, 9), "out" },
	{ 0.94, CFrame.new(0.06, -0.08, 0.05) * deg(-2, 0, 7), "impact" },
	{ 1.06, CFrame.new(-0.03, -0.18, 0.06) * deg(-7, 0, -3), "out" },
	{ 1.18, CFrame.identity, "impact" },
}
MAGFED.reload_emergency_b.magHide = { { 0.34, true }, { 0.68, false } }

MAGFED.reload_emergency_c = cloneAnimation(MAGFED.reload_emergency)
MAGFED.reload_emergency_c.durationScale = 0.96
MAGFED.reload_emergency_c.chanceWeight = 0.70
MAGFED.reload_emergency_c.weapon = {
	{ 0.00, CFrame.identity },
	{ 0.12, CFrame.new(0.04, -0.01, 0.24) * deg(10, 22, -15), "sharp" },
	{ 1.06, CFrame.new(0.04, -0.01, 0.24) * deg(10, 22, -15), "linear" },
	{ 1.28, CFrame.identity, "impact" },
	{ 1.48, CFrame.identity, "settle" },
}

MAGFED.reload_emergency_d = cloneAnimation(MAGFED.reload_emergency)
MAGFED.reload_emergency_d.durationScale = 1.07
MAGFED.reload_emergency_d.left = {
	{ 0.00, "left" },
	{ 0.12, "release", "impact" },
	{ 0.34, "pouch", "out" },
	{ 0.60, "pouch", "linear" },
	{ 0.90, { grip = "magwell", offset = CFrame.new(-0.05, -0.22, 0.10) }, "sharp" },
	{ 1.08, "magwell", "impact" },
	{ 1.18, { grip = "magwell", offset = CFrame.new(0, -0.07, 0.02) }, "out" },
	{ 1.26, "magwell", "impact" },
	{ 1.48, "left", "out" },
}

-- Empty reload variations -----------------------------------------------------
MAGFED.reload_empty_b = cloneAnimation(MAGFED.reload_empty)
MAGFED.reload_empty_b.length = 1.62
MAGFED.reload_empty_b.weapon = cloneAnimation(MAGFED.reload_tactical_b).weapon
MAGFED.reload_empty_b.bolt = MAGFED.reload_empty.bolt

MAGFED.reload_empty_c = cloneAnimation(MAGFED.reload_empty)
MAGFED.reload_empty_c.durationScale = 1.17
MAGFED.reload_empty_c.chanceWeight = 0.55
MAGFED.reload_empty_c.fumbleAt = 0.69
MAGFED.reload_empty_c.left = {
	{ 0.00, "left" },
	{ 0.10, "release", "impact" },
	{ 0.34, "pouch", "sharp" },
	{ 0.72, { grip = "magwell", offset = CFrame.new(-0.13, -0.54, 0.24) }, "out" },
	{ 0.96, { grip = "magwell", offset = CFrame.new(0.07, -0.08, 0.06) * deg(-3, 0, 8) }, "impact" },
	{ 1.08, { grip = "magwell", offset = CFrame.new(-0.04, -0.22, 0.07) }, "out" },
	{ 1.20, "magwell", "impact" },
	{ 1.36, "release", "impact" },
	{ 1.56, "left", "out" },
}
MAGFED.reload_empty_c.mag = {
	{ 0.00, CFrame.identity },
	{ 0.12, CFrame.new(0, -0.12, 0.03) * deg(-12, 0, 0), "out" },
	{ 0.30, CFrame.new(0.04, -1.55, 0.20) * deg(-48, 8, 12), "sharp" },
	{ 0.72, CFrame.new(-0.13, -0.54, 0.24) * deg(-22, 8, 10), "out" },
	{ 0.96, CFrame.new(0.07, -0.08, 0.06) * deg(-3, 0, 8), "impact" },
	{ 1.08, CFrame.new(-0.04, -0.22, 0.07) * deg(-8, 0, -3), "out" },
	{ 1.20, CFrame.identity, "impact" },
}
MAGFED.reload_empty_c.magHide = { { 0.34, true }, { 0.70, false } }
MAGFED.reload_empty_c.bolt = {
	{ 0.00, 1 },
	{ 1.34, 1, "linear" },
	{ 1.39, 0, "impact" },
}

-- Charging-handle finish instead of a bolt-release slap.
MAGFED.reload_empty_d = cloneAnimation(MAGFED.reload_empty)
MAGFED.reload_empty_d.durationScale = 1.08
MAGFED.reload_empty_d.left = {
	{ 0.00, "left" },
	{ 0.10, "release", "impact" },
	{ 0.38, "pouch", "out" },
	{ 0.82, { grip = "magwell", offset = CFrame.new(-0.12, -0.52, 0.24) }, "out" },
	{ 1.02, "magwell", "impact" },
	{ 1.16, "charge", "out" },
	{ 1.34, { grip = "charge", offset = CFrame.new(0, 0, 0.18) }, "out" },
	{ 1.44, "charge", "impact" },
	{ 1.56, "left", "out" },
}
MAGFED.reload_empty_d.bolt = {
	{ 0.00, 1 },
	{ 1.14, 1, "linear" },
	{ 1.34, 0.45, "out" },
	{ 1.44, 0, "impact" },
}

MAGFED.reload_empty_e = cloneAnimation(MAGFED.reload_empty)
MAGFED.reload_empty_e.length = 1.62
MAGFED.reload_empty_e.durationScale = 1.06
MAGFED.reload_empty_e.left = cloneAnimation(MAGFED.reload_tactical_e).left
MAGFED.reload_empty_e.left[#MAGFED.reload_empty_e.left] = { 1.56, "left", "out" }
MAGFED.reload_empty_e.bolt = MAGFED.reload_empty.bolt

MAGFED.reload_empty_f = cloneAnimation(MAGFED.reload_empty)
MAGFED.reload_empty_f.durationScale = 1.12
MAGFED.reload_empty_f.chanceWeight = 0.70
MAGFED.reload_empty_f.left = {
	{ 0.00, "left" },
	{ 0.10, "release", "impact" },
	{ 0.40, "pouch", "out" },
	{ 0.66, "pouch", "linear" },
	{ 0.76, { grip = "pouch", offset = CFrame.new(-0.07, -0.05, 0.04) * deg(0, 0, -7) }, "impact" },
	{ 0.94, { grip = "magwell", offset = CFrame.new(-0.14, -0.58, 0.25) }, "sharp" },
	{ 1.12, "magwell", "impact" },
	{ 1.34, "release", "impact" },
	{ 1.56, "left", "out" },
}
MAGFED.reload_empty_f.bolt = MAGFED.reload_empty.bolt

MAGFED.check_mag = {
	length = 0.85,
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.18, CFrame.new(-0.006, -0.008, 0.004) * deg(0.25, -0.15, 0.25), "out" },
		{ 0.62, CFrame.new(-0.006, -0.008, 0.004) * deg(0.25, -0.15, 0.25), "linear" },
		{ 0.85, CFrame.identity, "out" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		{ 0.18, CFrame.new(0.03, -0.10, 0.08) * deg(4, 24, -21), "out" },
		{ 0.62, CFrame.new(0.03, -0.10, 0.08) * deg(4, 24, -21), "linear" },
		{ 0.85, CFrame.identity, "quad" },
	},
	left = {
		{ 0.00, "left" },
		{ 0.16, "magwell", "out" },
		{ 0.34, { grip = "magwell", offset = CFrame.new(0, -0.26, 0) }, "quad" },
		{ 0.56, { grip = "magwell", offset = CFrame.new(0, -0.26, 0) }, "linear" },
		{ 0.70, "magwell", "snap" },
		{ 0.85, "left", "quad" },
	},
	right = { { 0.00, "right" } },
	mag = {
		{ 0.00, CFrame.identity },
		{ 0.34, CFrame.new(0, -0.26, 0.02), "quad" },
		{ 0.56, CFrame.new(0, -0.26, 0.02), "linear" },
		{ 0.70, CFrame.identity, "back" },
	},
}

MAGFED.check_chamber = {
	length = 0.60,
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.14, CFrame.new(0.004, -0.006, 0.002) * deg(0.18, 0.22, -0.18), "out" },
		{ 0.42, CFrame.new(0.004, -0.006, 0.002) * deg(0.18, 0.22, -0.18), "linear" },
		{ 0.60, CFrame.identity, "out" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		-- Rolls the ejection port up toward the eye.
		{ 0.14, CFrame.new(-0.03, -0.06, 0.06) * deg(2, -27, 19), "out" },
		{ 0.42, CFrame.new(-0.03, -0.06, 0.06) * deg(2, -27, 19), "linear" },
		{ 0.60, CFrame.identity, "quad" },
	},
	left = {
		{ 0.00, "left" },
		{ 0.14, "charge", "out" },
		{ 0.30, { grip = "charge", offset = CFrame.new(0, 0, 0.16) }, "quad" },
		{ 0.40, "charge", "snap" },
		{ 0.60, "left", "quad" },
	},
	right = { { 0.00, "right" } },
	bolt = {
		{ 0.00, 0 },
		{ 0.30, 0.38, "quad" },
		{ 0.42, 0, "snap" },
	},
}

MAGFED.inspect = {
	length = 2.10,
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.35, CFrame.new(-0.008, -0.010, 0.008) * deg(0.30, -0.25, 0.35), "out" },
		{ 1.32, CFrame.new(0.008, -0.008, 0.006) * deg(0.20, 0.30, -0.30), "out" },
		{ 2.10, CFrame.identity, "out" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		{ 0.35, CFrame.new(0.05, -0.12, 0.20) * deg(6, 36, -27), "out" },
		{ 0.85, CFrame.new(0.05, -0.12, 0.20) * deg(6, 36, -27), "linear" },
		{ 1.32, CFrame.new(-0.05, -0.10, 0.22) * deg(-5, -42, 25), "quad" },
		{ 1.70, CFrame.new(-0.05, -0.10, 0.22) * deg(-5, -42, 25), "linear" },
		{ 2.10, CFrame.identity, "quad" },
	},
	left = {
		{ 0.00, "left" },
		{ 0.40, "magwell", "out" },
		{ 1.24, "magwell", "linear" },
		{ 1.56, "left", "quad" },
	},
	right = { { 0.00, "right" } },
}

MAGFED.swap = {
	length = 0.85,
	camera = {
		{ 0.00, CFrame.new(0, -0.012, 0.018) * deg(0.5, 0, 0) },
		{ 0.55, CFrame.new(0, 0.006, -0.010) * deg(-0.35, 0, 0), "out" },
		{ 0.85, CFrame.identity, "settle" },
	},
	weapon = {
		{ 0.00, CFrame.new(0.10, -0.85, 0.20) * deg(-55, 30, 18) },
		{ 0.55, CFrame.new(0.02, -0.08, 0.03) * deg(-6, 4, 2), "out" },
		{ 0.85, CFrame.identity, "quad" },
	},
	left = {
		{ 0.00, "pouch" },
		{ 0.50, "left", "out" },
	},
	right = { { 0.00, "right" } },
}


MAGFED.fire_selector = {
	length = 0.18,
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.08, CFrame.new(0, -0.002, 0.002) * deg(0.08, 0, -0.08), "impact" },
		{ 0.18, CFrame.identity, "settle" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		{ 0.07, CFrame.new(-0.008, 0.004, 0.012) * deg(-0.6, -1.5, 1.2), "impact" },
		{ 0.18, CFrame.identity, "settle" },
	},
	left = { { 0.00, "left" } },
	right = {
		{ 0.00, "right" },
		{ 0.07, { grip = "right", offset = CFrame.new(-0.010, 0.006, 0.004) * deg(0, 0, -5) }, "impact" },
		{ 0.18, "right", "settle" },
	},
}


--------------------------------------------------------------------------------
-- TUBE-FED SET -- the M870. Inherits the checks and the inspect from above.
--------------------------------------------------------------------------------
local TUBE = {}
for k, v in MAGFED do
	TUBE[k] = v
end
TUBE.reload_tactical = nil
TUBE.reload_empty = nil

-- Working the action. The support hand does not need its own keyframes: it is
-- pinned to the pump, so it rides back and forward with the fore-end.
TUBE.pump = {
	length = 0.54,
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.17, CFrame.new(0, -0.012, 0.020) * deg(0.65, 0.12, 0), "impact" },
		{ 0.40, CFrame.new(0, 0.010, -0.028) * deg(-0.85, -0.10, 0), "impact" },
		{ 0.54, CFrame.identity, "settle" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		-- The gun is dragged backward by the rearward stroke, then punched
		-- forward as the fore-end bottoms out. Two impacts, 0.2 s apart.
		{ 0.14, CFrame.new(0, -0.035, 0.075) * deg(-4, 5, 0), "impact" },
		{ 0.34, CFrame.new(0, 0.010, -0.030) * deg(2, -2, 0), "impact" },
		{ 0.50, CFrame.identity, "settle" },
	},
	left = { { 0.00, { grip = "left", follow = "pump" } } },
	right = {
		{ 0.00, "right" },
		{ 0.16, { grip = "right", offset = CFrame.new(0, -0.010, 0.014) }, "impact" },
		{ 0.50, "right", "settle" },
	},
	-- Yanked back (t^3 accelerates, which is what a hand ripping a fore-end
	-- does) and slammed forward into a dead stop.
	pump = {
		{ 0.00, 0 },
		{ 0.17, 1, "sharp" },
		{ 0.20, 1, "linear" }, -- bottomed out at the rear for one frame
		{ 0.40, 0, "impact" },
	},
}

-- One shell. Played on a loop for as long as the player keeps feeding it, so
-- breaking off mid-load just stops the loop wherever it is.
local SHELL_TILT = CFrame.new(0.06, -0.17, 0.12) * deg(9, 27, -23)
TUBE.shells_load = {
	length = 0.52,
	loop = true,
	camera = {
		{ 0.00, CFrame.new(-0.006, -0.010, 0.006) * deg(0.25, -0.18, 0.26) },
		{ 0.28, CFrame.new(-0.008, -0.014, 0.010) * deg(0.35, -0.22, 0.32), "out" },
		{ 0.52, CFrame.new(-0.006, -0.010, 0.006) * deg(0.25, -0.18, 0.26), "out" },
	},
	weapon = { { 0.00, SHELL_TILT } },
	left = {
		{ 0.00, "pouch" },
		{ 0.24, "magwell", "out" }, -- magwell is the loading port on a shotgun
		{ 0.34, "magwell", "linear" },
		{ 0.52, "pouch", "quad" },
	},
	right = { { 0.00, "right" } },
	pump = { { 0.00, 0 } },
}


-- Shotgun-specific checks. Reusing a detachable-magazine animation for a tube
-- gun was visually wrong: the support hand was reaching for a magazine that
-- does not exist. These poses expose the loading/ejection ports instead.

--------------------------------------------------------------------------------
-- M870 SHELL-LOAD VARIATIONS
--------------------------------------------------------------------------------
TUBE.shells_load_b = cloneAnimation(TUBE.shells_load)
TUBE.shells_load_b.weapon = { { 0.00, CFrame.new(0.02, -0.09, 0.20) * deg(12, 19, -18) } }
TUBE.shells_load_b.left = {
	{ 0.00, "pouch" },
	{ 0.16, { grip = "magwell", offset = CFrame.new(-0.08, -0.18, 0.08) }, "out" },
	{ 0.31, "magwell", "impact" },
	{ 0.39, "magwell", "linear" },
	{ 0.52, "pouch", "out" },
}

-- Thumb slips off the shell base and immediately re-indexes it against the port.
TUBE.shells_load_c = cloneAnimation(TUBE.shells_load)
TUBE.shells_load_c.durationScale = 1.16
TUBE.shells_load_c.chanceWeight = 0.45
TUBE.shells_load_c.startScale = 1.05
TUBE.shells_load_c.left = {
	{ 0.00, "pouch" },
	{ 0.20, { grip = "magwell", offset = CFrame.new(-0.05, -0.13, 0.07) }, "out" },
	{ 0.29, { grip = "magwell", offset = CFrame.new(0.06, -0.03, 0.04) * deg(0, 0, 10) }, "impact" },
	{ 0.37, { grip = "magwell", offset = CFrame.new(-0.04, -0.11, 0.05) }, "out" },
	{ 0.45, "magwell", "impact" },
	{ 0.52, "pouch", "out" },
}

-- Under-receiver load with a stronger weapon roll and a short visual check of
-- the loading gate before the thumb commits.
TUBE.shells_load_d = cloneAnimation(TUBE.shells_load)
TUBE.shells_load_d.durationScale = 1.08
TUBE.shells_load_d.weapon = { { 0.00, CFrame.new(0.08, -0.10, 0.21) * deg(15, 34, -30) } }
TUBE.shells_load_d.left = {
	{ 0.00, "pouch" },
	{ 0.22, { grip = "magwell", offset = CFrame.new(0, -0.10, 0.05) }, "out" },
	{ 0.34, { grip = "magwell", offset = CFrame.new(0, -0.03, 0.01) }, "linear" },
	{ 0.43, "magwell", "impact" },
	{ 0.52, "pouch", "out" },
}

-- Fast practiced feed: shell starts higher and the hand snaps directly back to
-- the carrier instead of tracing a wide loop.
TUBE.shells_load_e = cloneAnimation(TUBE.shells_load)
TUBE.shells_load_e.durationScale = 0.94
TUBE.shells_load_e.chanceWeight = 0.65
TUBE.shells_load_e.left = {
	{ 0.00, { grip = "pouch", offset = CFrame.new(0.06, 0.16, -0.04) } },
	{ 0.23, "magwell", "sharp" },
	{ 0.34, "magwell", "impact" },
	{ 0.52, { grip = "pouch", offset = CFrame.new(0.06, 0.16, -0.04) }, "out" },
}

TUBE.check_mag = {
	length = 0.82,
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.20, CFrame.new(-0.006, -0.010, 0.008) * deg(0.30, -0.18, 0.30), "out" },
		{ 0.62, CFrame.new(-0.006, -0.010, 0.008) * deg(0.30, -0.18, 0.30), "linear" },
		{ 0.82, CFrame.identity, "out" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		{ 0.20, CFrame.new(0.05, -0.08, 0.16) * deg(8, 30, -28), "heavy" },
		{ 0.62, CFrame.new(0.05, -0.08, 0.16) * deg(8, 30, -28), "linear" },
		{ 0.82, CFrame.identity, "out" },
	},
	left = {
		{ 0.00, { grip = "left", follow = "pump" } },
		{ 0.22, "magwell", "out" },
		{ 0.58, "magwell", "linear" },
		{ 0.82, { grip = "left", follow = "pump" }, "out" },
	},
	right = { { 0.00, "right" } },
	pump = { { 0.00, 0 } },
}

TUBE.check_chamber = {
	length = 0.66,
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.18, CFrame.new(0.004, -0.006, 0.006) * deg(0.25, 0.18, -0.20), "out" },
		{ 0.48, CFrame.new(0.004, -0.006, 0.006) * deg(0.25, 0.18, -0.20), "linear" },
		{ 0.66, CFrame.identity, "out" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		{ 0.18, CFrame.new(-0.03, -0.05, 0.10) * deg(3, -24, 20), "heavy" },
		{ 0.48, CFrame.new(-0.03, -0.05, 0.10) * deg(3, -24, 20), "linear" },
		{ 0.66, CFrame.identity, "out" },
	},
	left = { { 0.00, { grip = "left", follow = "pump" } } },
	right = { { 0.00, "right" } },
	pump = {
		{ 0.00, 0 },
		{ 0.30, 0.34, "out" },
		{ 0.46, 0.34, "linear" },
		{ 0.56, 0, "impact" },
	},
}

TUBE.inspect = {
	length = 2.10,
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.36, CFrame.new(-0.008, -0.012, 0.008) * deg(0.35, -0.25, 0.38), "out" },
		{ 1.25, CFrame.new(0.006, -0.010, 0.008) * deg(0.25, 0.22, -0.30), "out" },
		{ 2.10, CFrame.identity, "out" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		{ 0.36, CFrame.new(0.07, -0.10, 0.20) * deg(7, 34, -27), "heavy" },
		{ 0.92, CFrame.new(0.07, -0.10, 0.20) * deg(7, 34, -27), "linear" },
		{ 1.25, CFrame.new(-0.05, -0.07, 0.22) * deg(-3, -34, 23), "out" },
		{ 1.72, CFrame.new(-0.05, -0.07, 0.22) * deg(-3, -34, 23), "linear" },
		{ 2.10, CFrame.identity, "out" },
	},
	left = {
		{ 0.00, { grip = "left", follow = "pump" } },
		{ 0.38, "magwell", "out" },
		{ 1.10, "magwell", "linear" },
		{ 1.42, { grip = "left", follow = "pump" }, "out" },
	},
	right = { { 0.00, "right" } },
	pump = {
		{ 0.00, 0 },
		{ 0.62, 0.28, "out" },
		{ 1.02, 0.28, "linear" },
		{ 1.20, 0, "impact" },
	},
}

TUBE.swap = {
	length = 0.92,
	camera = {
		{ 0.00, CFrame.new(0, -0.014, 0.018) * deg(0.55, 0, 0) },
		{ 0.50, CFrame.new(-0.006, -0.004, 0.006) * deg(0.18, -0.18, 0.22), "out" },
		{ 0.76, CFrame.new(0.004, 0.008, -0.016) * deg(-0.48, 0.12, -0.18), "impact" },
		{ 0.92, CFrame.identity, "settle" },
	},
	weapon = {
		-- A long gun clears the sling low and muzzle-up, then the stock is driven
		-- into the shoulder. The shotgun takes longer to arrest than the carbine.
		{ 0.00, CFrame.new(0.34, -1.08, 0.30) * deg(-54, 26, -24) },
		{ 0.48, CFrame.new(0.10, -0.28, 0.12) * deg(-18, 8, -9), "heavy" },
		{ 0.72, CFrame.identity, "impact" },
		{ 0.92, CFrame.identity, "settle" },
	},
	left = {
		{ 0.00, "pouch" },
		{ 0.42, "magwell", "out" },
		{ 0.68, { grip = "left", follow = "pump" }, "impact" },
		{ 0.92, { grip = "left", follow = "pump" }, "settle" },
	},
	right = {
		{ 0.00, "right" },
		{ 0.70, { grip = "right", offset = CFrame.new(0, -0.012, 0.012) }, "impact" },
		{ 0.92, "right", "settle" },
	},
	pump = { { 0.00, 0 } },
}

TUBE.fire_selector = {
	length = 0.18,
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.08, CFrame.new(0, -0.002, 0.002) * deg(0.08, 0, -0.08), "impact" },
		{ 0.18, CFrame.identity, "settle" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		{ 0.08, CFrame.new(-0.006, 0.003, 0.010) * deg(-0.4, -1.0, 0.8), "impact" },
		{ 0.18, CFrame.identity, "settle" },
	},
	left = { { 0.00, { grip = "left", follow = "pump" } } },
	right = {
		{ 0.00, "right" },
		{ 0.08, { grip = "right", offset = CFrame.new(-0.008, 0.004, 0.004) * deg(0, 0, -4) }, "impact" },
		{ 0.18, "right", "settle" },
	},
}

--------------------------------------------------------------------------------
-- PISTOL SET -- authored separately from the rifle.
--
-- Sharing one magazine-fed set made the sidearm move like a shrunken carbine:
-- the gun rolled around a shoulder stock it does not have, the support hand
-- reached for an AR charging handle, and the reload travelled far too much.
-- The pistol stays near the eye line, the firing hand remains the primary
-- support, and the support hand performs short, compact manipulations.
--------------------------------------------------------------------------------
local PISTOL = {}

local PISTOL_WORK = CFrame.new(-0.08, -0.08, 0.15) * deg(10, -18, 20)
local PISTOL_SEAT = CFrame.new(-0.10, -0.10, 0.18) * deg(12, -24, 24)

PISTOL.reload_tactical = {
	length = 1.34,
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.16, CFrame.new(0.006, -0.010, 0.008) * deg(0.28, 0.20, -0.25), "out" },
		{ 0.88, CFrame.new(0.006, -0.010, 0.008) * deg(0.28, 0.20, -0.25), "linear" },
		{ 1.18, CFrame.new(-0.004, 0.008, -0.014) * deg(-0.42, -0.12, 0.18), "impact" },
		{ 1.34, CFrame.identity, "settle" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		{ 0.06, CFrame.new(0, 0.018, -0.010) * deg(-2, 2, -2), "out" },
		{ 0.16, PISTOL_WORK, "heavy" },
		{ 0.76, PISTOL_SEAT, "linear" },
		{ 1.02, CFrame.new(-0.04, -0.04, 0.07) * deg(5, -8, 9), "out" },
		{ 1.18, CFrame.identity, "impact" },
		{ 1.34, CFrame.identity, "settle" },
	},
	left = {
		{ 0.00, "left" },
		{ 0.12, "magwell", "impact" },
		{ 0.22, "magwell", "linear" },
		{ 0.50, "pouch", "out" },
		{ 0.66, "pouch", "linear" },
		{ 0.88, { grip = "magwell", offset = CFrame.new(-0.10, -0.40, 0.16) }, "out" },
		{ 1.02, { grip = "magwell", offset = CFrame.new(0, -0.12, 0.04) }, "linear" },
		{ 1.10, "magwell", "impact" },
		{ 1.18, "left", "out" },
		{ 1.34, "left", "settle" },
	},
	right = {
		{ 0.00, "right" },
		{ 0.18, { grip = "right", offset = CFrame.new(0, -0.008, 0.005) * deg(0, 0, 2) }, "out" },
		{ 1.08, { grip = "right", offset = CFrame.new(0, -0.008, 0.005) * deg(0, 0, 2) }, "linear" },
		{ 1.22, "right", "impact" },
	},
	mag = {
		{ 0.00, CFrame.identity },
		{ 0.18, CFrame.identity, "linear" },
		{ 0.26, CFrame.new(0, -0.16, 0.03) * deg(-8, 0, 0), "out" },
		{ 0.50, CFrame.new(-0.32, -0.76, 0.24) * deg(-28, 12, 18), "out" },
		{ 0.66, CFrame.new(-0.32, -0.76, 0.24) * deg(-28, 12, 18), "linear" },
		{ 0.88, CFrame.new(-0.10, -0.40, 0.16) * deg(-18, 7, 9), "out" },
		{ 1.02, CFrame.new(0, -0.12, 0.04) * deg(-6, 0, 0), "linear" },
		{ 1.10, CFrame.identity, "impact" },
	},
	magHide = { { 0.60, true }, { 0.74, false } },
}

PISTOL.reload_tactical_alt_unavailable = {
	length = 1.24,
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.14, CFrame.new(0.004, -0.008, 0.006) * deg(0.22, 0.15, -0.20), "out" },
		{ 0.82, CFrame.new(0.004, -0.008, 0.006) * deg(0.22, 0.15, -0.20), "linear" },
		{ 1.08, CFrame.new(-0.004, 0.006, -0.012) * deg(-0.35, -0.10, 0.15), "impact" },
		{ 1.24, CFrame.identity, "settle" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		{ 0.14, CFrame.new(-0.05, -0.05, 0.10) * deg(7, -12, 14), "heavy" },
		{ 0.82, CFrame.new(-0.05, -0.05, 0.10) * deg(7, -12, 14), "linear" },
		{ 1.08, CFrame.identity, "impact" },
		{ 1.24, CFrame.identity, "settle" },
	},
	left = {
		{ 0.00, "left" },
		{ 0.18, "pouch", "out" },
		{ 0.34, "pouch", "linear" },
		{ 0.62, { grip = "magwell", offset = CFrame.new(-0.08, -0.26, 0.11) }, "out" },
		{ 0.78, { grip = "magwell", offset = CFrame.new(0, -0.11, 0.04) }, "linear" },
		{ 0.90, "magwell", "impact" },
		{ 1.06, "left", "out" },
		{ 1.24, "left", "settle" },
	},
	right = { { 0.00, "right" } },
	mag = {
		{ 0.00, CFrame.identity },
		{ 0.48, CFrame.identity, "linear" },
		{ 0.56, CFrame.new(0.02, -0.54, 0.08) * deg(-28, 4, 7), "sharp" },
		{ 0.62, CFrame.new(-0.08, -0.26, 0.11) * deg(-16, 6, 8), "linear" },
		{ 0.78, CFrame.new(0, -0.11, 0.04) * deg(-6, 0, 0), "linear" },
		{ 0.90, CFrame.identity, "impact" },
	},
	magHide = { { 0.58, true }, { 0.62, false } },
}

PISTOL.reload_empty = {
	length = 1.18,
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.12, CFrame.new(0.005, -0.009, 0.007) * deg(0.28, 0.18, -0.22), "sharp" },
		{ 0.74, CFrame.new(0.005, -0.009, 0.007) * deg(0.28, 0.18, -0.22), "linear" },
		{ 1.00, CFrame.new(-0.005, 0.009, -0.018) * deg(-0.55, -0.12, 0.20), "impact" },
		{ 1.18, CFrame.identity, "settle" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		{ 0.12, PISTOL_WORK, "sharp" },
		{ 0.74, PISTOL_SEAT, "linear" },
		{ 0.94, CFrame.new(-0.04, -0.02, 0.10) * deg(-3, -7, 8), "out" },
		{ 1.02, CFrame.identity, "impact" },
		{ 1.18, CFrame.identity, "settle" },
	},
	left = {
		{ 0.00, "left" },
		{ 0.10, "release", "impact" },
		{ 0.18, "pouch", "out" },
		{ 0.36, "pouch", "linear" },
		{ 0.62, { grip = "magwell", offset = CFrame.new(-0.10, -0.40, 0.16) }, "out" },
		{ 0.78, { grip = "magwell", offset = CFrame.new(0, -0.12, 0.04) }, "linear" },
		{ 0.88, "magwell", "impact" },
		{ 0.96, "release", "out" },
		{ 1.02, "release", "impact" },
		{ 1.18, "left", "out" },
	},
	right = { { 0.00, "right" } },
	mag = {
		{ 0.00, CFrame.identity },
		{ 0.10, CFrame.identity, "linear" },
		{ 0.18, CFrame.new(0.01, -0.38, 0.05) * deg(-20, 2, 4), "sharp" },
		{ 0.34, CFrame.new(0.04, -1.24, 0.14) * deg(-48, 8, 12), "sharp" },
		{ 0.62, CFrame.new(-0.10, -0.40, 0.16) * deg(-18, 7, 9), "out" },
		{ 0.78, CFrame.new(0, -0.12, 0.04) * deg(-6, 0, 0), "linear" },
		{ 0.88, CFrame.identity, "impact" },
	},
	magHide = { { 0.36, true }, { 0.62, false } },
	bolt = {
		{ 0.00, 1 },
		{ 0.98, 1, "linear" },
		{ 1.02, 0, "impact" },
	},
}


--------------------------------------------------------------------------------
-- P320 RELOAD VARIATIONS
--------------------------------------------------------------------------------
PISTOL.reload_emergency = cloneAnimation(PISTOL.reload_tactical_alt_unavailable)
PISTOL.reload_emergency.weapon = {
	{ 0.00, CFrame.identity },
	{ 0.12, CFrame.new(-0.04, -0.01, 0.18) * deg(9, -12, 16), "sharp" },
	{ 0.78, CFrame.new(-0.04, -0.01, 0.18) * deg(9, -12, 16), "linear" },
	{ 1.08, CFrame.identity, "impact" },
	{ 1.24, CFrame.identity, "settle" },
}

-- Centreline reload, gun almost directly beneath the sight picture.
PISTOL.reload_tactical_b = cloneAnimation(PISTOL.reload_tactical)
PISTOL.reload_tactical_b.weapon = {
	{ 0.00, CFrame.identity },
	{ 0.14, CFrame.new(-0.02, -0.01, 0.23) * deg(13, -8, 13), "heavy" },
	{ 0.84, CFrame.new(-0.02, -0.01, 0.23) * deg(13, -8, 13), "linear" },
	{ 1.10, CFrame.new(-0.01, 0, 0.07) * deg(3, -3, 4), "out" },
	{ 1.20, CFrame.identity, "impact" },
	{ 1.34, CFrame.identity, "settle" },
}

-- Magazine catches the heel of the grip and is re-indexed on the second drive.
PISTOL.reload_tactical_c = cloneAnimation(PISTOL.reload_tactical)
PISTOL.reload_tactical_c.durationScale = 1.18
PISTOL.reload_tactical_c.chanceWeight = 0.55
PISTOL.reload_tactical_c.fumbleAt = 0.72
PISTOL.reload_tactical_c.left = {
	{ 0.00, "left" },
	{ 0.12, "magwell", "impact" },
	{ 0.48, "pouch", "out" },
	{ 0.66, "pouch", "linear" },
	{ 0.88, { grip = "magwell", offset = CFrame.new(-0.10, -0.38, 0.16) }, "out" },
	{ 1.02, { grip = "magwell", offset = CFrame.new(0.05, -0.04, 0.05) * deg(-3, 0, 9) }, "impact" },
	{ 1.12, { grip = "magwell", offset = CFrame.new(-0.03, -0.15, 0.05) }, "out" },
	{ 1.22, "magwell", "impact" },
	{ 1.34, "left", "out" },
}
PISTOL.reload_tactical_c.mag = {
	{ 0.00, CFrame.identity },
	{ 0.20, CFrame.new(0, -0.15, 0.03) * deg(-8, 0, 0), "out" },
	{ 0.48, CFrame.new(-0.32, -0.76, 0.24) * deg(-28, 12, 18), "out" },
	{ 0.66, CFrame.new(-0.32, -0.76, 0.24) * deg(-28, 12, 18), "linear" },
	{ 0.88, CFrame.new(-0.10, -0.38, 0.16) * deg(-18, 7, 9), "out" },
	{ 1.02, CFrame.new(0.05, -0.04, 0.05) * deg(-3, 0, 9), "impact" },
	{ 1.12, CFrame.new(-0.03, -0.15, 0.05) * deg(-7, 0, -3), "out" },
	{ 1.22, CFrame.identity, "impact" },
}
PISTOL.reload_tactical_c.magHide = { { 0.58, true }, { 0.74, false } }

-- Garment/pouch snag: a short lateral rip frees the fresh pistol magazine.
PISTOL.reload_tactical_d = cloneAnimation(PISTOL.reload_tactical)
PISTOL.reload_tactical_d.durationScale = 1.10
PISTOL.reload_tactical_d.chanceWeight = 0.70
PISTOL.reload_tactical_d.left = {
	{ 0.00, "left" },
	{ 0.12, "magwell", "impact" },
	{ 0.48, "pouch", "out" },
	{ 0.68, "pouch", "linear" },
	{ 0.76, { grip = "pouch", offset = CFrame.new(-0.06, -0.03, 0.03) * deg(0, 0, -8) }, "impact" },
	{ 0.92, { grip = "magwell", offset = CFrame.new(-0.10, -0.40, 0.16) }, "sharp" },
	{ 1.08, "magwell", "impact" },
	{ 1.34, "left", "out" },
}

-- Firm baseplate confirmation after seating.
PISTOL.reload_tactical_e = cloneAnimation(PISTOL.reload_tactical)
PISTOL.reload_tactical_e.durationScale = 1.06
PISTOL.reload_tactical_e.left = {
	{ 0.00, "left" },
	{ 0.12, "magwell", "impact" },
	{ 0.50, "pouch", "out" },
	{ 0.68, "pouch", "linear" },
	{ 0.92, { grip = "magwell", offset = CFrame.new(-0.08, -0.32, 0.14) }, "out" },
	{ 1.08, "magwell", "impact" },
	{ 1.18, { grip = "magwell", offset = CFrame.new(0, -0.06, 0.02) }, "out" },
	{ 1.25, "magwell", "impact" },
	{ 1.34, "left", "out" },
}

PISTOL.reload_tactical_f = cloneAnimation(PISTOL.reload_tactical_b)
PISTOL.reload_tactical_f.durationScale = 0.94
PISTOL.reload_tactical_f.chanceWeight = 0.65

-- Emergency pistol variations ------------------------------------------------
PISTOL.reload_emergency_b = cloneAnimation(PISTOL.reload_emergency)
PISTOL.reload_emergency_b.length = 1.34
PISTOL.reload_emergency_b.durationScale = 1.12
PISTOL.reload_emergency_b.chanceWeight = 0.60
PISTOL.reload_emergency_b.fumbleAt = 0.68
PISTOL.reload_emergency_b.left = cloneAnimation(PISTOL.reload_tactical_c).left
PISTOL.reload_emergency_b.mag = cloneAnimation(PISTOL.reload_tactical_c).mag
PISTOL.reload_emergency_b.magHide = { { 0.30, true }, { 0.58, false } }

PISTOL.reload_emergency_c = cloneAnimation(PISTOL.reload_emergency)
PISTOL.reload_emergency_c.durationScale = 0.95
PISTOL.reload_emergency_c.chanceWeight = 0.70

PISTOL.reload_emergency_d = cloneAnimation(PISTOL.reload_emergency)
PISTOL.reload_emergency_d.length = 1.34
PISTOL.reload_emergency_d.durationScale = 1.06
PISTOL.reload_emergency_d.left = cloneAnimation(PISTOL.reload_tactical_e).left

-- Empty pistol variations -----------------------------------------------------
PISTOL.reload_empty_b = cloneAnimation(PISTOL.reload_empty)
PISTOL.reload_empty_b.length = 1.34
PISTOL.reload_empty_b.weapon = cloneAnimation(PISTOL.reload_tactical_b).weapon
PISTOL.reload_empty_b.bolt = PISTOL.reload_empty.bolt

PISTOL.reload_empty_c = cloneAnimation(PISTOL.reload_empty)
PISTOL.reload_empty_c.length = 1.34
PISTOL.reload_empty_c.durationScale = 1.18
PISTOL.reload_empty_c.chanceWeight = 0.50
PISTOL.reload_empty_c.fumbleAt = 0.66
PISTOL.reload_empty_c.left = cloneAnimation(PISTOL.reload_tactical_c).left
PISTOL.reload_empty_c.mag = cloneAnimation(PISTOL.reload_tactical_c).mag
PISTOL.reload_empty_c.bolt = {
	{ 0.00, 1 },
	{ 1.06, 1, "linear" },
	{ 1.11, 0, "impact" },
}

-- Overhand slide rack instead of using the slide release.
PISTOL.reload_empty_d = cloneAnimation(PISTOL.reload_empty)
PISTOL.reload_empty_d.durationScale = 1.10
PISTOL.reload_empty_d.left = {
	{ 0.00, "left" },
	{ 0.10, "release", "impact" },
	{ 0.30, "pouch", "sharp" },
	{ 0.70, { grip = "magwell", offset = CFrame.new(-0.08, -0.32, 0.14) }, "out" },
	{ 0.88, "magwell", "impact" },
	{ 0.98, "charge", "out" },
	{ 1.08, { grip = "charge", offset = CFrame.new(0, 0, 0.12) }, "out" },
	{ 1.16, "charge", "impact" },
	{ 1.18, "left", "out" },
}
PISTOL.reload_empty_d.bolt = {
	{ 0.00, 1 },
	{ 0.96, 1, "linear" },
	{ 1.08, 0.45, "out" },
	{ 1.16, 0, "impact" },
}

PISTOL.reload_empty_e = cloneAnimation(PISTOL.reload_empty)
PISTOL.reload_empty_e.length = 1.34
PISTOL.reload_empty_e.durationScale = 1.06
PISTOL.reload_empty_e.left = cloneAnimation(PISTOL.reload_tactical_e).left
PISTOL.reload_empty_e.bolt = PISTOL.reload_empty.bolt

PISTOL.check_mag = {
	length = 0.72,
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.16, CFrame.new(0.004, -0.006, 0.004) * deg(0.16, 0.14, -0.16), "out" },
		{ 0.54, CFrame.new(0.004, -0.006, 0.004) * deg(0.16, 0.14, -0.16), "linear" },
		{ 0.72, CFrame.identity, "out" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		{ 0.16, CFrame.new(-0.04, -0.04, 0.10) * deg(6, -14, 16), "out" },
		{ 0.54, CFrame.new(-0.04, -0.04, 0.10) * deg(6, -14, 16), "linear" },
		{ 0.72, CFrame.identity, "out" },
	},
	left = {
		{ 0.00, "left" },
		{ 0.14, "magwell", "out" },
		{ 0.30, { grip = "magwell", offset = CFrame.new(0, -0.18, 0.03) }, "out" },
		{ 0.50, { grip = "magwell", offset = CFrame.new(0, -0.18, 0.03) }, "linear" },
		{ 0.60, "magwell", "impact" },
		{ 0.72, "left", "out" },
	},
	right = { { 0.00, "right" } },
	mag = {
		{ 0.00, CFrame.identity },
		{ 0.30, CFrame.new(0, -0.18, 0.03), "out" },
		{ 0.50, CFrame.new(0, -0.18, 0.03), "linear" },
		{ 0.60, CFrame.identity, "impact" },
	},
}

PISTOL.check_chamber = {
	length = 0.56,
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.14, CFrame.new(0.004, -0.005, 0.003) * deg(0.16, 0.12, -0.14), "out" },
		{ 0.40, CFrame.new(0.004, -0.005, 0.003) * deg(0.16, 0.12, -0.14), "linear" },
		{ 0.56, CFrame.identity, "out" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		{ 0.14, CFrame.new(-0.03, -0.02, 0.08) * deg(4, -20, 18), "out" },
		{ 0.40, CFrame.new(-0.03, -0.02, 0.08) * deg(4, -20, 18), "linear" },
		{ 0.56, CFrame.identity, "out" },
	},
	left = {
		{ 0.00, "left" },
		{ 0.14, "charge", "out" },
		{ 0.28, { grip = "charge", offset = CFrame.new(0, 0, 0.10) }, "out" },
		{ 0.40, "charge", "impact" },
		{ 0.56, "left", "out" },
	},
	right = { { 0.00, "right" } },
	bolt = {
		{ 0.00, 0 },
		{ 0.28, 0.42, "out" },
		{ 0.40, 0, "impact" },
	},
}

PISTOL.inspect = {
	length = 2.10,
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.30, CFrame.new(0.004, -0.008, 0.006) * deg(0.22, 0.16, -0.20), "out" },
		{ 1.15, CFrame.new(-0.004, -0.006, 0.004) * deg(0.18, -0.14, 0.18), "out" },
		{ 2.10, CFrame.identity, "out" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		{ 0.30, CFrame.new(-0.04, -0.03, 0.14) * deg(7, -36, 28), "out" },
		{ 0.84, CFrame.new(-0.04, -0.03, 0.14) * deg(7, -36, 28), "linear" },
		{ 1.15, CFrame.new(0.05, -0.02, 0.16) * deg(-5, 42, -24), "out" },
		{ 1.68, CFrame.new(0.05, -0.02, 0.16) * deg(-5, 42, -24), "linear" },
		{ 2.10, CFrame.identity, "out" },
	},
	left = {
		{ 0.00, "left" },
		{ 0.32, "magwell", "out" },
		{ 1.20, "magwell", "linear" },
		{ 1.54, "left", "out" },
	},
	right = { { 0.00, "right" } },
}

PISTOL.swap = {
	length = 0.72,
	camera = {
		{ 0.00, CFrame.new(0, -0.010, 0.014) * deg(0.40, 0, 0) },
		{ 0.42, CFrame.new(0, 0.004, -0.008) * deg(-0.25, 0, 0), "out" },
		{ 0.72, CFrame.identity, "settle" },
	},
	weapon = {
		{ 0.00, CFrame.new(0.24, -0.92, 0.26) * deg(-60, -18, -28) },
		{ 0.40, CFrame.new(0.04, -0.10, 0.05) * deg(-8, -3, -4), "out" },
		{ 0.58, CFrame.identity, "impact" },
		{ 0.72, CFrame.identity, "settle" },
	},
	left = {
		{ 0.00, "pouch" },
		{ 0.50, "left", "out" },
	},
	right = { { 0.00, "right" } },
}

PISTOL.fire_selector = {
	length = 0.16,
	camera = {
		{ 0.00, CFrame.identity },
		{ 0.07, CFrame.new(0, -0.001, 0.002) * deg(0.06, 0, -0.06), "impact" },
		{ 0.16, CFrame.identity, "settle" },
	},
	weapon = {
		{ 0.00, CFrame.identity },
		{ 0.07, CFrame.new(-0.005, 0.003, 0.008) * deg(-0.4, -1.0, 0.8), "impact" },
		{ 0.16, CFrame.identity, "settle" },
	},
	left = {
		{ 0.00, "left" },
		{ 0.07, { grip = "release", offset = CFrame.new(0, 0.01, 0) }, "impact" },
		{ 0.16, "left", "out" },
	},
	right = { { 0.00, "right" } },
}

-- ACTION FRAMING ------------------------------------------------------------
-- Keyframed weapon motion is authored relative to the normal ready pose, but
-- reload work needs to occupy the centre third of the screen. This separate
-- screen-space presentation layer moves the whole weapon, both hands and both
-- magazines together; it does not alter the mechanical bone tracks.
-- Screen framing must move the action UP without moving it INTO the near clip
-- plane. Positive local Z points back toward the camera in this rig. v11 used
-- +0.16 and then several reload poses added another +0.25; the magazine and
-- receiver ended up only a few tenths of a stud from the eye and filled the
-- entire screen. The negative Z values below preserve a readable workspace.
local RIFLE_ACTION_SCREEN = CFrame.new(-0.06, 0.15, -0.20) * deg(-1, -2, 1)
local PISTOL_ACTION_SCREEN = CFrame.new(-0.04, 0.12, -0.16) * deg(-1, -2, 1)
local SHOTGUN_ACTION_SCREEN = CFrame.new(-0.05, 0.13, -0.18) * deg(-1, -2, 1)

local function frameActions(set: any, screenCF: CFrame)
	for name, action in set do
		if type(action) == "table" and (
			name:find("^reload_")
			or name == "check_mag"
			or name == "check_chamber"
			or name == "inspect"
			or name == "load_shell"
		) then
			action.screen = screenCF
		end
	end
end

frameActions(MAGFED, RIFLE_ACTION_SCREEN)
frameActions(PISTOL, PISTOL_ACTION_SCREEN)
frameActions(TUBE, SHOTGUN_ACTION_SCREEN)

-- TECHNIQUE PRESENTATION ----------------------------------------------------
-- Every reload now has its own moving screen-space workspace, not one constant
-- framing offset. This is the layer the eye reads first: one technique works
-- high and close, another low by the belt, another across the chest. The
-- weapon, both hands and both magazine models move together through this track.
local function actionScreen(
	length: number,
	enterTime: number,
	workCF: CFrame,
	exitTime: number,
	settleCF: CFrame?
)
	return {
		{ 0.00, CFrame.identity },
		{ enterTime * 0.35, CFrame.new(0, 0.018, -0.018) * deg(-1.2, 0, 0), "out" },
		{ enterTime, workCF, "heavy" },
		{ exitTime, workCF, "linear" },
		{ math.min(length, exitTime + 0.10), settleCF or (workCF:Lerp(CFrame.identity, 0.72)), "impact" },
		{ length, CFrame.identity, "settle" },
	}
end

-- Clean tactical cycle. The two fumbles are explicitly excluded from the core
-- cycle by `fumbleAt`, so pressing R repeatedly gives six clean, visibly
-- different techniques before a rare interruption is allowed.
MAGFED.reload_tactical.cycleOrder = 2
MAGFED.reload_tactical_b.cycleOrder = 1
MAGFED.reload_tactical_d.fumbleAt = 0.53
MAGFED.reload_tactical_e.cycleOrder = 5
MAGFED.reload_tactical_f.cycleOrder = 3
MAGFED.reload_tactical_g.cycleOrder = 6
MAGFED.reload_tactical_h.cycleOrder = 4

MAGFED.reload_tactical.screen = actionScreen(
	MAGFED.reload_tactical.length, 0.18,
	CFrame.new(-0.13, 0.16, -0.28) * deg(-3, 8, -5),
	1.30
)
MAGFED.reload_tactical_b.screen = actionScreen(
	MAGFED.reload_tactical_b.length, 0.20,
	CFrame.new(-0.22, 0.25, -0.36) * deg(-5, -8, 5),
	1.32
)
MAGFED.reload_tactical_c.screen = actionScreen(
	MAGFED.reload_tactical_c.length, 0.18,
	CFrame.new(-0.04, 0.20, -0.33) * deg(-2, 13, -8),
	1.38
)
MAGFED.reload_tactical_d.screen = actionScreen(
	MAGFED.reload_tactical_d.length, 0.20,
	CFrame.new(-0.18, 0.10, -0.30) * deg(2, -6, 4),
	1.38
)
MAGFED.reload_tactical_e.screen = actionScreen(
	MAGFED.reload_tactical_e.length, 0.20,
	CFrame.new(-0.02, 0.25, -0.38) * deg(-6, 6, -4),
	1.46
)
MAGFED.reload_tactical_f.screen = actionScreen(
	MAGFED.reload_tactical_f.length, 0.20,
	CFrame.new(0.14, 0.17, -0.35) * deg(-3, 18, -10),
	1.32
)
MAGFED.reload_tactical_g.screen = actionScreen(
	MAGFED.reload_tactical_g.length, 0.14,
	CFrame.new(-0.16, 0.27, -0.40) * deg(-7, -10, 6),
	1.34
)
MAGFED.reload_tactical_h.screen = actionScreen(
	MAGFED.reload_tactical_h.length, 0.22,
	CFrame.new(-0.24, 0.30, -0.42) * deg(-10, -13, 8),
	1.50
)

-- Elbow choreography is part of the silhouette. L-index opens the support
-- elbow, cross-body retention tucks it in, and the high workspace lifts it.
MAGFED.reload_tactical.leftPole = {
	{ 0.00, Vector3.zero },
	{ 0.24, Vector3.new(-0.12, -0.10, 0.04), "out" },
	{ 0.82, Vector3.new(-0.30, -0.08, 0.16), "out" },
	{ 1.38, Vector3.new(-0.08, -0.04, 0.03), "out" },
	{ 1.62, Vector3.zero, "settle" },
}
MAGFED.reload_tactical_b.leftPole = {
	{ 0.00, Vector3.zero },
	{ 0.24, Vector3.new(-0.40, 0.02, 0.22), "out" },
	{ 1.16, Vector3.new(-0.34, 0.02, 0.18), "linear" },
	{ 1.62, Vector3.zero, "settle" },
}
MAGFED.reload_tactical_f.leftPole = {
	{ 0.00, Vector3.zero },
	{ 0.24, Vector3.new(0.16, -0.16, 0.06), "out" },
	{ 1.18, Vector3.new(0.22, -0.12, 0.10), "linear" },
	{ 1.62, Vector3.zero, "settle" },
}
MAGFED.reload_tactical_h.leftPole = {
	{ 0.00, Vector3.zero },
	{ 0.28, Vector3.new(-0.18, 0.24, 0.20), "out" },
	{ 1.38, Vector3.new(-0.18, 0.18, 0.16), "linear" },
	{ 1.72, Vector3.zero, "settle" },
}

-- Empty reloads are not clones anymore at presentation level. Each finish has
-- a different workspace and elbow route: bolt paddle, charging handle, seat
-- check, or a disturbed pouch draw.
MAGFED.reload_empty.technique = "drop_bolt_paddle"
MAGFED.reload_empty.cycleOrder = 1
MAGFED.reload_empty_b.technique = "high_drop_bolt_paddle"
MAGFED.reload_empty_b.cycleOrder = 2
MAGFED.reload_empty_c.technique = "empty_magwell_miss"
MAGFED.reload_empty_c.cycleOrder = 6
MAGFED.reload_empty_d.technique = "charging_handle_finish"
MAGFED.reload_empty_d.cycleOrder = 3
MAGFED.reload_empty_e.technique = "seat_check_bolt_paddle"
MAGFED.reload_empty_e.cycleOrder = 4
MAGFED.reload_empty_f.technique = "empty_pouch_snag"
MAGFED.reload_empty_f.cycleOrder = 5
MAGFED.reload_empty_f.fumbleAt = 0.49

MAGFED.reload_empty.screen = actionScreen(
	MAGFED.reload_empty.length, 0.14,
	CFrame.new(0.04, 0.18, -0.31) * deg(-2, 12, -7),
	1.34
)
MAGFED.reload_empty_b.screen = actionScreen(
	MAGFED.reload_empty_b.length, 0.18,
	CFrame.new(-0.20, 0.27, -0.40) * deg(-7, -8, 5),
	1.40
)
MAGFED.reload_empty_c.screen = actionScreen(
	MAGFED.reload_empty_c.length, 0.17,
	CFrame.new(-0.02, 0.22, -0.36) * deg(-4, 14, -8),
	1.42
)
MAGFED.reload_empty_d.screen = actionScreen(
	MAGFED.reload_empty_d.length, 0.16,
	CFrame.new(0.15, 0.24, -0.39) * deg(-6, 20, -11),
	1.44
)
MAGFED.reload_empty_e.screen = actionScreen(
	MAGFED.reload_empty_e.length, 0.18,
	CFrame.new(-0.10, 0.25, -0.39) * deg(-6, 4, -3),
	1.46
)
MAGFED.reload_empty_f.screen = actionScreen(
	MAGFED.reload_empty_f.length, 0.18,
	CFrame.new(-0.19, 0.11, -0.31) * deg(1, -7, 5),
	1.40
)
MAGFED.reload_empty_d.leftPole = {
	{ 0.00, Vector3.zero },
	{ 0.86, Vector3.new(-0.10, 0.12, 0.24), "out" },
	{ 1.42, Vector3.new(-0.28, 0.22, 0.30), "out" },
	{ 1.56, Vector3.zero, "settle" },
}
MAGFED.reload_empty_e.leftPole = {
	{ 0.00, Vector3.zero },
	{ 0.28, Vector3.new(-0.30, -0.04, 0.14), "out" },
	{ 1.42, Vector3.new(-0.18, -0.02, 0.08), "linear" },
	{ 1.62, Vector3.zero, "settle" },
}

-- Emergency reloads cycle independently from empty reloads. The fumbled
-- emergency remains rare; the three clean variants use separate presentation.
MAGFED.reload_emergency.technique = "straight_drop"
MAGFED.reload_emergency.cycleOrder = 1
MAGFED.reload_emergency_b.technique = "emergency_magwell_miss"
MAGFED.reload_emergency_b.cycleOrder = 4
MAGFED.reload_emergency_c.technique = "fast_drop"
MAGFED.reload_emergency_c.cycleOrder = 2
MAGFED.reload_emergency_d.technique = "drop_seat_check"
MAGFED.reload_emergency_d.cycleOrder = 3
MAGFED.reload_emergency.screen = actionScreen(
	MAGFED.reload_emergency.length, 0.12,
	CFrame.new(0.08, 0.16, -0.31) * deg(-2, 15, -9),
	1.28
)
MAGFED.reload_emergency_b.screen = actionScreen(
	MAGFED.reload_emergency_b.length, 0.14,
	CFrame.new(-0.02, 0.21, -0.35) * deg(-4, 13, -8),
	1.32
)
MAGFED.reload_emergency_c.screen = actionScreen(
	MAGFED.reload_emergency_c.length, 0.10,
	CFrame.new(-0.14, 0.25, -0.39) * deg(-6, -5, 4),
	1.24
)
MAGFED.reload_emergency_d.screen = actionScreen(
	MAGFED.reload_emergency_d.length, 0.14,
	CFrame.new(0.13, 0.22, -0.36) * deg(-4, 19, -10),
	1.32
)

-- Pistol techniques use different wrist workspaces rather than inheriting a
-- rifle-shaped presentation.
PISTOL.reload_tactical.cycleOrder = 1
PISTOL.reload_tactical_b.cycleOrder = 2
PISTOL.reload_tactical_d.fumbleAt = 0.50
PISTOL.reload_tactical_e.cycleOrder = 3
PISTOL.reload_tactical_f.cycleOrder = 4
PISTOL.reload_tactical.screen = actionScreen(
	PISTOL.reload_tactical.length, 0.16,
	CFrame.new(-0.10, 0.18, -0.24) * deg(-3, 8, -4),
	1.16
)
PISTOL.reload_tactical_b.screen = actionScreen(
	PISTOL.reload_tactical_b.length, 0.15,
	CFrame.new(0.09, 0.22, -0.27) * deg(-5, 18, -9),
	1.12
)
PISTOL.reload_tactical_c.screen = actionScreen(
	PISTOL.reload_tactical_c.length, 0.16,
	CFrame.new(-0.02, 0.21, -0.28) * deg(-4, 12, -7),
	1.18
)
PISTOL.reload_tactical_d.screen = actionScreen(
	PISTOL.reload_tactical_d.length, 0.18,
	CFrame.new(-0.16, 0.13, -0.25) * deg(1, -5, 4),
	1.20
)
PISTOL.reload_tactical_e.screen = actionScreen(
	PISTOL.reload_tactical_e.length, 0.16,
	CFrame.new(0.02, 0.24, -0.30) * deg(-6, 14, -8),
	1.24
)
PISTOL.reload_tactical_f.screen = actionScreen(
	PISTOL.reload_tactical_f.length, 0.12,
	CFrame.new(-0.12, 0.26, -0.31) * deg(-7, -4, 3),
	1.12
)

-- The tube-fed gun also cycles visibly between strong-side, centreline,
-- rolled-over and fast double-feed workspaces.
TUBE.shells_load.screen = actionScreen(
	TUBE.shells_load.length, 0.16,
	CFrame.new(-0.04, 0.13, -0.26) * deg(-2, 10, -7),
	0.84
)
TUBE.shells_load_b.screen = actionScreen(
	TUBE.shells_load_b.length, 0.16,
	CFrame.new(-0.17, 0.17, -0.30) * deg(-4, -5, 5),
	0.84
)
TUBE.shells_load_c.screen = actionScreen(
	TUBE.shells_load_c.length, 0.17,
	CFrame.new(0.08, 0.12, -0.27) * deg(0, 18, -12),
	0.86
)
TUBE.shells_load_d.screen = actionScreen(
	TUBE.shells_load_d.length, 0.16,
	CFrame.new(0.13, 0.19, -0.31) * deg(-5, 26, -16),
	0.86
)
TUBE.shells_load_e.screen = actionScreen(
	TUBE.shells_load_e.length, 0.12,
	CFrame.new(-0.10, 0.20, -0.32) * deg(-5, 1, -2),
	0.80
)


--------------------------------------------------------------------------------
-- V15 HUMAN RIFLE RELOAD LIBRARY
--
-- The earlier passes accumulated variants by cloning one timeline. Different
-- labels and screen offsets cannot hide identical hand order, identical elbow
-- rhythm and identical stops. These ten clean techniques are authored from two
-- genuinely different mechanical graphs (old-mag-first and fresh-mag-first),
-- with distinct workspaces, shoulder participation and event timing. Two rare
-- interruptions remain outside the clean cycle.
--------------------------------------------------------------------------------
local function cf(x: number, y: number, z: number, pitch: number, yaw: number, roll: number): CFrame
	return CFrame.new(x, y, z) * deg(pitch, yaw, roll)
end

local function shoulderTracks(length: number, left: Vector3, right: Vector3)
	return {
		left = {
			{ 0.00, Vector3.zero },
			{ length * 0.16, left * 0.55, "out" },
			{ length * 0.34, left, "heavy" },
			{ length * 0.76, left, "linear" },
			{ length, Vector3.zero, "settle" },
		},
		right = {
			{ 0.00, Vector3.zero },
			{ length * 0.22, right, "out" },
			{ length * 0.78, right, "linear" },
			{ length, Vector3.zero, "settle" },
		},
	}
end

local function elbowPoleTrack(length: number, peak: Vector3, startAt: number?, endAt: number?)
	local a = startAt or 0.12
	local b = endAt or 0.84
	return {
		{ 0.00, Vector3.zero },
		{ length * a, peak * 0.45, "out" },
		{ length * math.min(a + 0.16, b), peak, "heavy" },
		{ length * b, peak, "linear" },
		{ length, Vector3.zero, "settle" },
	}
end

local function makeOldFirstTac(spec: any): any
	local L = spec.length
	local outT = spec.magOut * L
	local acquireT = spec.acquire * L
	local seatT = spec.magIn * L
	local recoverT = math.min(L - 0.10, seatT + L * 0.13)
	local shoulder = shoulderTracks(L, spec.leftShoulder, spec.rightShoulder)
	local work = spec.work
	local work2 = spec.work2 or (work * cf(0.01, 0.015, -0.015, -1.2, 1.6, -1.0))
	local oldOffset = spec.oldOffset or cf(-0.05, -0.38, 0.10, -15, 4, -8)
	local pouchMag = spec.pouchMag or cf(-0.13, -0.92, 0.20, -20, 7, -10)
	local freshStart = spec.freshStart or cf(-0.12, -0.72, 0.18, -13, 5, -7)
	local freshApproach = spec.freshApproach or cf(-0.04, -0.26, 0.07, -7, 2, -3)
	-- Body-local pouch target, measured from the shoulder plane. Unlike the old
	-- `grips.pouch`, this point stays on the plate carrier while the rifle rolls.
	local bodyPouch = spec.bodyPouch or cf(-0.34, -1.05, -0.10, -42, 18, 12)

	local anim = {
		length = L,
		technique = spec.technique,
		cycleOrder = spec.cycleOrder,
		durationScale = spec.durationScale or 1,
		chanceWeight = spec.chanceWeight or 1,
		fumbleAt = spec.fumbleAt,
		humanity = spec.humanity or {},
		screen = actionScreen(L, L * 0.14, spec.screen, L * 0.82, spec.screen:Lerp(CFrame.identity, 0.68)),
		camera = {
			{ 0.00, CFrame.identity },
			{ L * 0.18, cf(-0.006, -0.010, 0.006, 0.24, -0.18, 0.28), "out" },
			{ L * 0.74, cf(-0.006, -0.010, 0.006, 0.24, -0.18, 0.28), "linear" },
			{ recoverT, cf(0.004, 0.006, -0.008, -0.30, 0.12, -0.18), "impact" },
			{ L, CFrame.identity, "settle" },
		},
		weapon = {
			{ 0.00, CFrame.identity },
			{ L * 0.045, cf(-0.006, 0.018, -0.018, -2.0, -2.2, 1.5), "anticipate" },
			{ L * 0.18, work, "heavy" },
			{ L * 0.31, work2, "out" },
			{ seatT - L * 0.05, work2, "linear" },
			{ seatT + L * 0.035, work2 * cf(0, 0.028, -0.030, -3.2, 0.8, -1.8), "impact" },
			{ recoverT, cf(0.035, 0.030, -0.10, 2.8, 8.0, -5.0), "out" },
			{ L, CFrame.identity, "settle" },
		},
		left = {
			{ 0.00, "left" },
			{ math.max(0.08, outT - L * 0.10), { grip = "magwell", offset = cf(-0.02, 0.04, 0.01, 4, 0, -7) }, "out" },
			{ outT, "magwell", "impact" },
			{ outT + L * 0.11, { grip = "magwell", offset = oldOffset }, "out" },
			{ acquireT - L * 0.08, { grip = "pouch", offset = spec.pouchHand or CFrame.identity }, "out" },
			{ acquireT, { grip = "pouch", offset = spec.pouchHand or CFrame.identity }, "linear" },
			{ seatT - L * 0.16, { grip = "magwell", offset = freshApproach }, "out" },
			{ seatT, "magwell", "impact" },
			{ seatT + L * 0.055, { grip = "magwell", offset = cf(0, 0.025, -0.015, -2, 0, 0) }, "settle" },
			{ L, "left", "out" },
		},
		right = {
			{ 0.00, "right" },
			{ L * 0.22, { grip = "right", offset = cf(0.008, -0.014, 0.006, 1.3, -1.2, 1.0) }, "out" },
			{ L * 0.74, { grip = "right", offset = cf(0.008, -0.014, 0.006, 1.3, -1.2, 1.0) }, "linear" },
			{ recoverT, { grip = "right", offset = cf(-0.005, 0.010, -0.012, -1.0, 0.8, -0.6) }, "impact" },
			{ L, "right", "settle" },
		},
		mag = {
			{ 0.00, CFrame.identity },
			{ outT, CFrame.identity, "linear" },
			{ outT + L * 0.11, oldOffset, "out" },
			{ acquireT - L * 0.08, pouchMag, "out" },
			{ acquireT + L * 0.02, pouchMag, "linear" },
			{ seatT - L * 0.16, freshStart, "snap" },
			{ seatT - L * 0.035, freshApproach, "out" },
			{ seatT, CFrame.identity, "impact" },
			{ L, CFrame.identity, "linear" },
		},
		magHide = {
			{ 0.00, false },
			{ acquireT + L * 0.015, true },
			{ seatT - L * 0.17, false },
		},
		leftBody = {
			{ 0.00, bodyPouch },
			{ outT + L * 0.08, bodyPouch, "linear" },
			{ acquireT + L * 0.04, bodyPouch * cf(0.02, 0.03, -0.02, 4, -3, 2), "out" },
			{ L, bodyPouch, "linear" },
		},
		leftBodyWeight = {
			{ 0.00, 0 },
			{ outT + L * 0.07, 0 },
			{ outT + L * 0.17, 1, "out" },
			{ acquireT + L * 0.035, 1, "linear" },
			{ seatT - L * 0.18, 0, "heavy" },
			{ L, 0, "linear" },
		},
		leftPole = elbowPoleTrack(L, spec.leftPole or Vector3.new(-0.08, -0.05, -0.10), 0.10, 0.86),
		rightPole = {
			{ 0.00, Vector3.zero },
			{ L * 0.25, spec.rightPole or Vector3.new(0.04, -0.03, 0.03), "out" },
			{ L * 0.78, spec.rightPole or Vector3.new(0.04, -0.03, 0.03), "linear" },
			{ L, Vector3.zero, "settle" },
		},
		leftShoulder = shoulder.left,
		rightShoulder = shoulder.right,
	}

	if spec.tap then
		table.insert(anim.left, #anim.left, { seatT + L * 0.105, { grip = "magwell", offset = cf(0, 0.05, -0.02, -4, 0, 0) }, "impact" })
		table.insert(anim.weapon, #anim.weapon, { seatT + L * 0.11, work2 * cf(0, 0.018, -0.018, -2.0, 0, -1.0), "impact" })
	end
	if spec.miss then
		local missT = (spec.contact or spec.magIn - 0.10) * L
		table.insert(anim.left, #anim.left - 1, { missT, { grip = "magwell", offset = cf(spec.missX or 0.11, -0.12, 0.02, -6, 4, 8) }, "impact" })
		table.insert(anim.mag, #anim.mag - 1, { missT, cf(spec.missX or 0.11, -0.16, 0.04, -8, 5, 10), "impact" })
	end
	return anim
end

local function makeFreshFirstTac(spec: any): any
	local L = spec.length
	local acquireT = spec.acquire * L
	local outT = spec.magOut * L
	local seatT = spec.magIn * L
	local recoverT = math.min(L - 0.10, seatT + L * 0.12)
	local shoulder = shoulderTracks(L, spec.leftShoulder, spec.rightShoulder)
	local work = spec.work
	local work2 = spec.work2 or (work * cf(-0.01, 0.012, -0.018, -1.0, -1.8, 1.4))
	local oldCarry = spec.oldCarry or cf(-0.16, -0.43, 0.14, -18, 10, -16)
	local spareStart = spec.spareStart or cf(-0.18, -0.76, 0.20, -15, 8, -12)
	local spareIndex = spec.spareIndex or cf(-0.11, -0.31, 0.10, -9, 5, -8)
	local bodyPouch = spec.bodyPouch or cf(-0.34, -1.05, -0.10, -42, 18, 12)

	return {
		length = L,
		technique = spec.technique,
		cycleOrder = spec.cycleOrder,
		durationScale = spec.durationScale or 1,
		humanity = spec.humanity or {},
		screen = actionScreen(L, L * 0.15, spec.screen, L * 0.83, spec.screen:Lerp(CFrame.identity, 0.70)),
		camera = {
			{ 0.00, CFrame.identity },
			{ L * 0.19, cf(-0.008, -0.012, 0.005, 0.30, -0.24, 0.34), "out" },
			{ L * 0.76, cf(-0.008, -0.012, 0.005, 0.30, -0.24, 0.34), "linear" },
			{ recoverT, cf(0.004, 0.005, -0.008, -0.28, 0.15, -0.20), "impact" },
			{ L, CFrame.identity, "settle" },
		},
		weapon = {
			{ 0.00, CFrame.identity },
			{ L * 0.05, cf(-0.006, 0.020, -0.020, -2.3, -2.0, 1.8), "anticipate" },
			{ L * 0.20, work, "heavy" },
			{ L * 0.35, work2, "out" },
			{ seatT - L * 0.05, work2, "linear" },
			{ seatT + L * 0.035, work2 * cf(0, 0.028, -0.032, -3.0, 0.8, -1.7), "impact" },
			{ recoverT, cf(0.030, 0.028, -0.11, 2.5, 7.0, -4.0), "out" },
			{ L, CFrame.identity, "settle" },
		},
		left = {
			{ 0.00, "left" },
			{ acquireT, { grip = "pouch", offset = spec.pouchHand or CFrame.identity }, "out" },
			{ acquireT + L * 0.10, { grip = "magwell", offset = spareStart }, "out" },
			{ outT - L * 0.07, { grip = "magwell", offset = spareIndex }, "out" },
			{ outT, "magwell", "impact" },
			{ outT + L * 0.10, { grip = "magwell", offset = oldCarry }, "out" },
			{ seatT - L * 0.08, { grip = "magwell", offset = cf(-0.03, -0.18, 0.06, -5, 2, -4) }, "out" },
			{ seatT, "magwell", "impact" },
			{ seatT + L * 0.06, { grip = "magwell", offset = cf(0, 0.025, -0.015, -2, 0, 0) }, "settle" },
			{ L, "left", "out" },
		},
		right = {
			{ 0.00, "right" },
			{ L * 0.23, { grip = "right", offset = cf(0.008, -0.014, 0.006, 1.2, -1.2, 1.0) }, "out" },
			{ L * 0.77, { grip = "right", offset = cf(0.008, -0.014, 0.006, 1.2, -1.2, 1.0) }, "linear" },
			{ recoverT, { grip = "right", offset = cf(-0.004, 0.010, -0.012, -1.0, 0.8, -0.6) }, "impact" },
			{ L, "right", "settle" },
		},
		mag = {
			{ 0.00, CFrame.identity },
			{ outT, CFrame.identity, "linear" },
			{ outT + L * 0.10, oldCarry, "out" },
			{ seatT + L * 0.04, oldCarry * cf(-0.04, -0.10, 0.02, -4, 0, -5), "linear" },
			{ seatT + L * 0.07, CFrame.identity, "snap" },
			{ L, CFrame.identity, "linear" },
		},
		spareMag = {
			{ 0.00, spareStart },
			{ acquireT, spareStart, "linear" },
			{ outT - L * 0.07, spareIndex, "out" },
			{ outT + L * 0.10, oldCarry * cf(0.10, 0.05, -0.04, 5, -5, 8), "out" },
			{ seatT - L * 0.08, cf(-0.03, -0.18, 0.06, -5, 2, -4), "out" },
			{ seatT, CFrame.identity, "impact" },
			{ L, CFrame.identity, "linear" },
		},
		magHide = { { 0.00, false }, { seatT + L * 0.045, true }, { seatT + L * 0.075, false } },
		spareMagHide = { { 0.00, true }, { acquireT - L * 0.03, false }, { seatT + L * 0.075, true } },
		leftBody = {
			{ 0.00, bodyPouch },
			{ acquireT + L * 0.055, bodyPouch * cf(0.02, 0.035, -0.02, 4, -2, 2), "out" },
			{ L, bodyPouch, "linear" },
		},
		leftBodyWeight = {
			{ 0.00, 0 },
			{ L * 0.045, 0 },
			{ acquireT * 0.72, 1, "out" },
			{ acquireT + L * 0.045, 1, "linear" },
			{ outT - L * 0.09, 0, "heavy" },
			{ L, 0, "linear" },
		},
		leftPole = elbowPoleTrack(L, spec.leftPole or Vector3.new(-0.08, -0.04, -0.11), 0.08, 0.86),
		rightPole = {
			{ 0.00, Vector3.zero },
			{ L * 0.26, spec.rightPole or Vector3.new(0.04, -0.03, 0.03), "out" },
			{ L * 0.78, spec.rightPole or Vector3.new(0.04, -0.03, 0.03), "linear" },
			{ L, Vector3.zero, "settle" },
		},
		leftShoulder = shoulder.left,
		rightShoulder = shoulder.right,
	}
end

local rifleTacSpecs = {
	-- 1: fresh magazine is acquired first and indexed alongside the old one.
	{ key = "reload_tactical", fresh = true, technique = "fresh_first_l_index", cycleOrder = 1, length = 1.68,
		acquire = 0.16, magOut = 0.43, magIn = 0.72,
		screen = cf(-0.14, 0.19, -0.50, -4, -5, 4), work = cf(-0.10, 0.11, -0.18, 8, 24, -18),
		bodyPouch = cf(-0.39, -1.02, -0.11, -45, 20, 14),
		leftShoulder = Vector3.new(-0.09, 0.03, -0.10), rightShoulder = Vector3.new(0.02, -0.01, -0.035),
		humanity = { leftArc = Vector3.new(-0.030, 0.020, 0.005), microRotDeg = 0.20 } },
	-- 2: conventional old-mag-first retention into the primary pouch.
	{ key = "reload_tactical_b", technique = "pouch_stow_human", cycleOrder = 2, length = 1.72,
		magOut = 0.20, acquire = 0.49, magIn = 0.76,
		screen = cf(-0.05, 0.16, -0.48, -2, 7, -5), work = cf(-0.13, 0.10, -0.20, 10, 30, -22),
		bodyPouch = cf(-0.31, -1.14, -0.08, -50, 16, 9),
		leftShoulder = Vector3.new(-0.07, -0.02, -0.08), rightShoulder = Vector3.new(0.015, -0.01, -0.03),
		humanity = { leftArc = Vector3.new(0.020, 0.010, -0.010), microRotDeg = 0.24 } },
	-- 3: cross-body retention; old magazine is carried inward across the plate.
	{ key = "reload_tactical_f", technique = "cross_body_human", cycleOrder = 3, length = 1.78,
		magOut = 0.20, acquire = 0.51, magIn = 0.77,
		screen = cf(0.10, 0.18, -0.53, -3, 14, -9), work = cf(0.02, 0.13, -0.22, 7, 36, -26),
		oldOffset = cf(0.20, -0.34, 0.08, -13, -8, 16), pouchMag = cf(0.26, -0.84, 0.18, -18, -9, 18),
		pouchHand = cf(0.20, 0.02, 0.02, 0, -8, 8),
		bodyPouch = cf(0.18, -0.91, -0.18, -38, -24, -15),
		leftShoulder = Vector3.new(0.02, -0.04, -0.12), rightShoulder = Vector3.new(0.025, -0.01, -0.04),
		humanity = { leftArc = Vector3.new(0.055, -0.005, -0.015), microRotDeg = 0.26 } },
	-- 4: high muzzle-up chest workspace, useful in tight geometry.
	{ key = "reload_tactical_h", technique = "muzzle_up_human", cycleOrder = 4, length = 1.84,
		magOut = 0.22, acquire = 0.52, magIn = 0.78,
		screen = cf(-0.18, 0.27, -0.60, -8, -8, 6), work = cf(-0.14, 0.16, -0.25, 29, 15, -18),
		bodyPouch = cf(-0.42, -0.78, -0.16, -34, 24, 17),
		leftShoulder = Vector3.new(-0.08, 0.07, -0.12), rightShoulder = Vector3.new(0.01, 0.03, -0.05),
		humanity = { leftArc = Vector3.new(-0.025, 0.055, 0.005), microPos = 0.005, microRotDeg = 0.24 } },
	-- 5: support-side workspace; gun rolls left while the stock remains shouldered.
	{ key = "reload_tactical_e", technique = "support_side_human", cycleOrder = 5, length = 1.75,
		magOut = 0.19, acquire = 0.47, magIn = 0.74,
		screen = cf(0.13, 0.16, -0.54, -3, 11, -8), work = cf(0.06, 0.11, -0.24, 5, -13, 18),
		bodyPouch = cf(-0.17, -1.00, -0.18, -43, 2, 2),
		oldOffset = cf(0.12, -0.40, 0.10, -16, -6, 14), pouchMag = cf(0.18, -0.88, 0.20, -20, -5, 15),
		leftShoulder = Vector3.new(0.04, -0.02, -0.10), rightShoulder = Vector3.new(0.02, -0.02, -0.03),
		tap = true, humanity = { leftArc = Vector3.new(0.045, 0.012, -0.010), microRotDeg = 0.22 } },
	-- 6: lower belt-line workspace with restrained rifle movement.
	{ key = "reload_tactical_g", technique = "belt_line_retention", cycleOrder = 6, length = 1.66,
		magOut = 0.18, acquire = 0.47, magIn = 0.75, durationScale = 0.97,
		screen = cf(-0.07, 0.08, -0.50, 1, 5, -3), work = cf(-0.10, 0.05, -0.22, 5, 27, -17),
		pouchMag = cf(-0.10, -0.98, 0.18, -22, 5, -8),
		bodyPouch = cf(-0.28, -1.25, -0.03, -55, 15, 8),
		leftShoulder = Vector3.new(-0.06, -0.06, -0.07), rightShoulder = Vector3.new(0.01, -0.02, -0.025),
		humanity = { leftArc = Vector3.new(-0.010, -0.020, -0.012), microRotDeg = 0.19 } },
	-- 7: high chest retention with a deliberate tactile seat check.
	{ key = "reload_tactical_i", technique = "high_chest_tactile", cycleOrder = 7, length = 1.86,
		magOut = 0.21, acquire = 0.50, magIn = 0.73, tap = true,
		screen = cf(-0.05, 0.28, -0.59, -7, 3, -2), work = cf(-0.09, 0.18, -0.25, 15, 24, -15),
		bodyPouch = cf(-0.32, -0.82, -0.14, -35, 17, 10),
		leftShoulder = Vector3.new(-0.05, 0.06, -0.11), rightShoulder = Vector3.new(0.01, 0.025, -0.045),
		humanity = { leftArc = Vector3.new(0.008, 0.050, 0.000), microPos = 0.004, microRotDeg = 0.18 } },
	-- 8: reverse L-index, new magazine comes up on a wider outside arc.
	{ key = "reload_tactical_j", fresh = true, technique = "reverse_l_index", cycleOrder = 8, length = 1.76,
		acquire = 0.17, magOut = 0.45, magIn = 0.74,
		screen = cf(0.08, 0.21, -0.56, -5, 10, -7), work = cf(0.02, 0.13, -0.23, 9, 31, -20),
		spareStart = cf(0.18, -0.78, 0.20, -16, -8, 14), spareIndex = cf(0.13, -0.34, 0.10, -10, -6, 12),
		bodyPouch = cf(-0.14, -1.03, -0.19, -42, -3, -2),
		oldCarry = cf(0.17, -0.44, 0.13, -18, -7, 15),
		leftShoulder = Vector3.new(0.03, 0.01, -0.12), rightShoulder = Vector3.new(0.02, 0.0, -0.04),
		humanity = { leftArc = Vector3.new(0.060, 0.020, -0.005), microRotDeg = 0.25 } },
	-- 9: compact under-arm retention; minimal muzzle excursion, more shoulder work.
	{ key = "reload_tactical_k", technique = "under_arm_retention", cycleOrder = 9, length = 1.70,
		magOut = 0.19, acquire = 0.48, magIn = 0.75,
		screen = cf(-0.16, 0.14, -0.57, -2, -10, 8), work = cf(-0.15, 0.09, -0.27, 7, 12, -8),
		oldOffset = cf(-0.18, -0.36, 0.12, -14, 10, -15), pouchMag = cf(-0.24, -0.86, 0.20, -18, 12, -18),
		bodyPouch = cf(-0.49, -0.94, -0.05, -44, 26, 19),
		leftShoulder = Vector3.new(-0.12, -0.01, -0.13), rightShoulder = Vector3.new(-0.01, -0.01, -0.05),
		humanity = { leftArc = Vector3.new(-0.050, 0.008, -0.012), microRotDeg = 0.21 } },
	-- 10: fast retention. Shorter path, no impossible teleport and no perfect timing.
	{ key = "reload_tactical_l", technique = "fast_retention_human", cycleOrder = 10, length = 1.58,
		magOut = 0.17, acquire = 0.45, magIn = 0.73, durationScale = 0.93,
		screen = cf(-0.08, 0.18, -0.52, -4, 4, -3), work = cf(-0.11, 0.12, -0.23, 9, 26, -18),
		bodyPouch = cf(-0.34, -1.06, -0.10, -43, 18, 11),
		leftShoulder = Vector3.new(-0.06, 0.01, -0.09), rightShoulder = Vector3.new(0.015, 0, -0.03),
		humanity = { leftLeadMin = 0.004, leftLeadMax = 0.020, leftArc = Vector3.new(0.004, 0.020, -0.006), microRotDeg = 0.28 } },
	-- Rare interruption 1: real magwell contact, re-index, then seat.
	{ key = "reload_tactical_c", technique = "magwell_fumble_human", length = 1.92,
		magOut = 0.19, acquire = 0.47, contact = 0.69, magIn = 0.82, durationScale = 1.14, fumbleAt = 0.69, miss = true,
		screen = cf(-0.03, 0.18, -0.55, -3, 8, -5), work = cf(-0.12, 0.12, -0.24, 10, 31, -22),
		leftShoulder = Vector3.new(-0.07, 0.0, -0.11), rightShoulder = Vector3.new(0.02, -0.01, -0.04),
		humanity = { leftArc = Vector3.new(0.018, 0.015, -0.012), microPos = 0.006, microRotDeg = 0.32 } },
	-- Rare interruption 2: pouch snag; the hand stalls low and breaks free.
	{ key = "reload_tactical_d", technique = "pouch_snag_human", length = 2.00,
		magOut = 0.19, acquire = 0.58, magIn = 0.83, durationScale = 1.17, fumbleAt = 0.58,
		screen = cf(-0.10, 0.12, -0.55, 0, 3, -2), work = cf(-0.12, 0.08, -0.24, 7, 26, -17),
		pouchMag = cf(-0.15, -1.02, 0.22, -24, 8, -10),
		leftShoulder = Vector3.new(-0.08, -0.08, -0.09), rightShoulder = Vector3.new(0.01, -0.02, -0.03),
		humanity = { leftArc = Vector3.new(-0.012, -0.030, -0.015), microPos = 0.006, microRotDeg = 0.30 } },
}


for _, spec in rifleTacSpecs do
	MAGFED[spec.key] = spec.fresh and makeFreshFirstTac(spec) or makeOldFirstTac(spec)
end

--------------------------------------------------------------------------------
-- V16 COMBAT EMPTY-RELOAD LIBRARY
--
-- This is the reload the user is actually testing: the bolt is locked rearward
-- after firing the last round. V15 put the large library on tactical reloads,
-- so repeatedly shooting empty still exposed the older six cloned timelines.
-- These twelve are authored around one trained doctrine: firing hand and stock
-- remain planted, support hand works from the carrier, muzzle stays downrange,
-- and only the workspace/cant, magazine index and bolt-finish vary.
--------------------------------------------------------------------------------
local function makeCombatEmpty(spec: any): any
	local L = spec.length
	local magOutT = spec.magOut * L
	local acquireT = spec.acquire * L
	local approachT = spec.approach * L
	local seatT = spec.magIn * L
	local boltT = spec.bolt * L
	local recoverT = math.min(L - 0.08, boltT + L * 0.10)
	local work = spec.work
	local work2 = spec.work2 or (work * cf(0.004, 0.010, -0.010, -1.0, 1.2, -0.8))
	local bodyPouch = spec.bodyPouch
	local freshStart = spec.freshStart or cf(-0.12, -0.62, 0.14, -13, 4, -8)
	local freshIndex = spec.freshIndex or cf(-0.04, -0.24, 0.055, -7, 2, -4)
	local drop = spec.drop or cf(0.03, -1.10, 0.12, -42, 6, 10)
	local shoulders = shoulderTracks(L, spec.leftShoulder, spec.rightShoulder)

	local left = {
		{ 0.00, "left" },
		{ L * 0.055, { grip = "left", offset = cf(-0.01, 0.025, -0.015, 2, 0, -3) }, "out" },
		{ approachT - L * 0.08, { grip = "magwell", offset = freshStart }, "out" },
		{ approachT, { grip = "magwell", offset = freshIndex }, "out" },
		{ seatT, "magwell", "impact" },
	}
	if spec.pushPull then
		table.insert(left, { seatT + L * 0.035, { grip = "magwell", offset = cf(0, -0.055, 0.015, 2, 0, 0) }, "out" })
		table.insert(left, { seatT + L * 0.075, "magwell", "impact" })
	end
	if spec.tap then
		table.insert(left, { seatT + L * 0.055, { grip = "magwell", offset = cf(0, 0.055, -0.018, -4, 0, 0) }, "out" })
		table.insert(left, { seatT + L * 0.095, "magwell", "impact" })
	end
	if spec.charge then
		table.insert(left, { boltT - L * 0.09, "charge", "out" })
		table.insert(left, { boltT, { grip = "charge", offset = cf(0, 0, 0.16, 0, 0, 0) }, "impact" })
		table.insert(left, { boltT + L * 0.045, "charge", "snap" })
	else
		table.insert(left, { boltT - L * 0.055, "release", "out" })
		table.insert(left, { boltT, "release", "impact" })
	end
	table.insert(left, { L, "left", "out" })
	table.sort(left, function(a, b)
		return a[1] < b[1]
	end)

	local anim = {
		length = L,
		technique = spec.technique,
		cycleOrder = spec.cycleOrder,
		durationScale = spec.durationScale or 1,
		humanity = {
			leftLeadMin = spec.leftLeadMin or 0.012,
			leftLeadMax = spec.leftLeadMax or 0.032,
			rightLagMin = 0.005,
			rightLagMax = 0.016,
			cameraLagMin = 0.004,
			cameraLagMax = 0.010,
			leftArc = spec.leftArc,
			rightArc = Vector3.new(0.002, 0.004, -0.002),
			microPos = 0.0008,
			microRotDeg = 0.05,
		},
		-- Screen motion is intentionally tiny. The old -0.50 to -0.60 Z offsets
		-- translated the entire rifle away from the camera and made the stock sweep
		-- through the head. Rotation below happens around the shoulder pocket.
		screen = {
			{ 0.00, CFrame.identity },
			{ L * 0.10, spec.screen, "out" },
			{ L * 0.84, spec.screen, "linear" },
			{ L, CFrame.identity, "settle" },
		},
		camera = {
			{ 0.00, CFrame.identity },
			{ L * 0.18, cf(-0.002, -0.003, 0.001, 0.08, -0.05, 0.08), "out" },
			{ L * 0.78, cf(-0.002, -0.003, 0.001, 0.08, -0.05, 0.08), "linear" },
			{ L, CFrame.identity, "settle" },
		},
		weapon = {
			{ 0.00, CFrame.identity },
			{ L * 0.035, cf(-0.002, 0.010, -0.006, -1.1, -0.8, 0.7), "anticipate" },
			{ L * 0.15, work, "out" },
			{ approachT, work2, "heavy" },
			{ seatT - L * 0.025, work2, "linear" },
			{ seatT + L * 0.025, work2 * cf(0, 0.018, -0.018, -2.2, 0.4, -1.2), "impact" },
			{ boltT + L * 0.025, work2 * cf(0.002, 0.010, -0.012, -1.4, 0.3, -0.7), "impact" },
			{ recoverT, cf(0.010, 0.012, -0.035, 1.0, 3.0, -1.8), "out" },
			{ L, CFrame.identity, "settle" },
		},
		left = left,
		right = {
			{ 0.00, "right" },
			-- Firing-hand index depresses the magazine release while the palm keeps
			-- the pistol grip and butt pad planted.
			{ magOutT, { grip = "right", offset = cf(0.006, -0.010, 0.004, 0.8, -0.8, 0.7) }, "impact" },
			{ boltT, { grip = "right", offset = cf(0.006, -0.010, 0.004, 0.8, -0.8, 0.7) }, "linear" },
			{ recoverT, { grip = "right", offset = cf(-0.003, 0.006, -0.008, -0.6, 0.4, -0.4) }, "out" },
			{ L, "right", "settle" },
		},
		mag = {
			{ 0.00, CFrame.identity },
			{ magOutT, CFrame.identity, "linear" },
			{ magOutT + L * 0.085, drop, "sharp" },
			{ approachT - L * 0.07, freshStart, "snap" },
			{ approachT, freshIndex, "out" },
			{ seatT, CFrame.identity, "impact" },
			{ L, CFrame.identity, "linear" },
		},
		magHide = {
			{ 0.00, false },
			{ magOutT + L * 0.09, true },
			{ approachT - L * 0.075, false },
		},
		bolt = {
			{ 0.00, 1 },
			{ boltT, 1, "linear" },
			{ boltT + L * 0.035, 0, "impact" },
		},
		leftBody = {
			{ 0.00, bodyPouch },
			{ L, bodyPouch, "linear" },
		},
		leftBodyWeight = {
			{ 0.00, 0 },
			{ L * 0.08, 0 },
			{ L * 0.16, 1, "out" },
			{ acquireT, 1, "linear" },
			{ approachT - L * 0.10, 1, "linear" },
			{ approachT, 0, "out" },
			{ L, 0, "linear" },
		},
		leftPole = elbowPoleTrack(L, spec.leftPole, 0.08, 0.90),
		rightPole = {
			{ 0.00, Vector3.zero },
			{ L * 0.18, spec.rightPole or Vector3.new(0.02, -0.01, 0.02), "out" },
			{ L * 0.84, spec.rightPole or Vector3.new(0.02, -0.01, 0.02), "linear" },
			{ L, Vector3.zero, "settle" },
		},
		leftShoulder = shoulders.left,
		rightShoulder = shoulders.right,
	}
	if spec.charge then
		anim.charge = {
			{ 0.00, 0 },
			{ boltT - L * 0.055, 0, "linear" },
			{ boltT, 1, "out" },
			{ boltT + L * 0.045, 0, "impact" },
		}
	end
	return anim
end

local combatEmptySpecs = {
	{ key = "reload_empty", technique = "combat_standard_bolt", cycleOrder = 1, length = 1.42,
		magOut = 0.10, acquire = 0.27, approach = 0.55, magIn = 0.70, bolt = 0.84,
		screen = cf(-0.025, 0.075, -0.055, 0, 0, 0), work = cf(-0.018, 0.045, -0.025, 6, 18, -12),
		bodyPouch = cf(-0.38, -1.05, -0.11, -44, 19, 12), leftPole = Vector3.new(-0.10, -0.03, 0.10),
		leftShoulder = Vector3.new(-0.05, 0.01, -0.07), rightShoulder = Vector3.new(0.01, 0, -0.02),
		leftArc = Vector3.new(-0.015, 0.028, -0.008) },
	{ key = "reload_empty_b", technique = "combat_compact_bolt", cycleOrder = 2, length = 1.36,
		magOut = 0.09, acquire = 0.25, approach = 0.53, magIn = 0.68, bolt = 0.83,
		screen = cf(-0.055, 0.055, -0.035, 0, 0, 0), work = cf(-0.035, 0.035, -0.020, 5, 12, -8),
		bodyPouch = cf(-0.44, -0.98, -0.07, -42, 24, 16), leftPole = Vector3.new(-0.16, -0.02, 0.12),
		leftShoulder = Vector3.new(-0.08, 0.0, -0.08), rightShoulder = Vector3.new(0.00, 0, -0.02),
		leftArc = Vector3.new(-0.030, 0.018, -0.004) },
	{ key = "reload_empty_c", technique = "combat_support_roll", cycleOrder = 3, length = 1.44,
		magOut = 0.10, acquire = 0.28, approach = 0.56, magIn = 0.71, bolt = 0.85,
		screen = cf(0.035, 0.070, -0.050, 0, 0, 0), work = cf(0.020, 0.050, -0.028, 5, -10, 15),
		bodyPouch = cf(-0.28, -1.08, -0.16, -45, 10, 6), leftPole = Vector3.new(0.04, -0.02, 0.13),
		leftShoulder = Vector3.new(0.02, 0.01, -0.08), rightShoulder = Vector3.new(0.01, 0, -0.025),
		leftArc = Vector3.new(0.035, 0.026, -0.010) },
	{ key = "reload_empty_d", technique = "combat_high_workspace", cycleOrder = 4, length = 1.48,
		magOut = 0.11, acquire = 0.29, approach = 0.57, magIn = 0.72, bolt = 0.86,
		screen = cf(-0.045, 0.115, -0.065, 0, 0, 0), work = cf(-0.035, 0.070, -0.030, 12, 14, -12),
		bodyPouch = cf(-0.40, -0.91, -0.15, -37, 22, 14), leftPole = Vector3.new(-0.12, 0.08, 0.14),
		leftShoulder = Vector3.new(-0.06, 0.05, -0.09), rightShoulder = Vector3.new(0.01, 0.02, -0.03),
		leftArc = Vector3.new(-0.018, 0.050, -0.006) },
	{ key = "reload_empty_e", technique = "combat_centerline", cycleOrder = 5, length = 1.40,
		magOut = 0.09, acquire = 0.26, approach = 0.54, magIn = 0.69, bolt = 0.84,
		screen = cf(0.000, 0.080, -0.045, 0, 0, 0), work = cf(-0.005, 0.050, -0.022, 7, 8, -6),
		bodyPouch = cf(-0.31, -1.12, -0.10, -48, 16, 9), leftPole = Vector3.new(-0.05, -0.04, 0.11),
		leftShoulder = Vector3.new(-0.03, -0.01, -0.07), rightShoulder = Vector3.new(0.01, 0, -0.02),
		leftArc = Vector3.new(0.000, 0.034, -0.012) },
	{ key = "reload_empty_f", technique = "combat_wide_pouch", cycleOrder = 6, length = 1.46,
		magOut = 0.10, acquire = 0.30, approach = 0.58, magIn = 0.72, bolt = 0.86,
		screen = cf(-0.020, 0.072, -0.050, 0, 0, 0), work = cf(-0.020, 0.047, -0.026, 6, 21, -14),
		bodyPouch = cf(-0.52, -1.08, -0.05, -46, 30, 20), leftPole = Vector3.new(-0.24, -0.03, 0.15),
		leftShoulder = Vector3.new(-0.12, 0.00, -0.09), rightShoulder = Vector3.new(0.01, 0, -0.025),
		leftArc = Vector3.new(-0.050, 0.026, -0.004) },
	{ key = "reload_empty_g", technique = "combat_push_pull", cycleOrder = 7, length = 1.49,
		magOut = 0.10, acquire = 0.27, approach = 0.54, magIn = 0.68, bolt = 0.86, pushPull = true, tap = true,
		screen = cf(-0.015, 0.085, -0.050, 0, 0, 0), work = cf(-0.020, 0.052, -0.026, 7, 17, -11),
		bodyPouch = cf(-0.36, -1.03, -0.12, -43, 19, 12), leftPole = Vector3.new(-0.08, -0.01, 0.12),
		leftShoulder = Vector3.new(-0.05, 0.01, -0.08), rightShoulder = Vector3.new(0.01, 0, -0.025),
		leftArc = Vector3.new(-0.010, 0.032, -0.010) },
	{ key = "reload_empty_h", technique = "combat_magwell_glance", cycleOrder = 8, length = 1.45,
		magOut = 0.10, acquire = 0.28, approach = 0.56, magIn = 0.71, bolt = 0.85,
		screen = cf(-0.035, 0.090, -0.055, 0, 0, 0), work = cf(-0.028, 0.055, -0.030, 9, 24, -17),
		bodyPouch = cf(-0.37, -1.02, -0.11, -43, 19, 12), leftPole = Vector3.new(-0.09, 0.01, 0.14),
		leftShoulder = Vector3.new(-0.05, 0.02, -0.09), rightShoulder = Vector3.new(0.01, 0, -0.025),
		leftArc = Vector3.new(-0.012, 0.038, -0.010) },
	{ key = "reload_empty_i", technique = "combat_underarm", cycleOrder = 9, length = 1.43,
		magOut = 0.09, acquire = 0.27, approach = 0.55, magIn = 0.70, bolt = 0.85,
		screen = cf(-0.070, 0.060, -0.035, 0, 0, 0), work = cf(-0.050, 0.040, -0.018, 5, 8, -5),
		bodyPouch = cf(-0.48, -0.98, -0.04, -43, 27, 19), leftPole = Vector3.new(-0.20, -0.03, 0.10),
		leftShoulder = Vector3.new(-0.11, 0.0, -0.09), rightShoulder = Vector3.new(-0.01, 0, -0.025),
		leftArc = Vector3.new(-0.042, 0.018, -0.006) },
	{ key = "reload_empty_j", technique = "combat_tactile_seat", cycleOrder = 10, length = 1.50,
		magOut = 0.10, acquire = 0.28, approach = 0.56, magIn = 0.70, bolt = 0.87, tap = true,
		screen = cf(-0.010, 0.095, -0.055, 0, 0, 0), work = cf(-0.015, 0.057, -0.028, 8, 15, -10),
		bodyPouch = cf(-0.35, -1.00, -0.13, -42, 18, 11), leftPole = Vector3.new(-0.07, 0.01, 0.13),
		leftShoulder = Vector3.new(-0.04, 0.02, -0.08), rightShoulder = Vector3.new(0.01, 0, -0.025),
		leftArc = Vector3.new(-0.006, 0.040, -0.010) },
	{ key = "reload_empty_k", technique = "combat_charging_handle_fallback", cycleOrder = 11, length = 1.60,
		magOut = 0.10, acquire = 0.27, approach = 0.54, magIn = 0.69, bolt = 0.84, charge = true,
		screen = cf(0.015, 0.095, -0.050, 0, 0, 0), work = cf(0.010, 0.058, -0.025, 8, 13, -9),
		bodyPouch = cf(-0.34, -1.04, -0.12, -44, 18, 11), leftPole = Vector3.new(-0.06, 0.05, 0.18),
		leftShoulder = Vector3.new(-0.04, 0.04, -0.10), rightShoulder = Vector3.new(0.01, 0.01, -0.03),
		leftArc = Vector3.new(0.000, 0.045, 0.000) },
	{ key = "reload_empty_l", technique = "combat_fast_bolt", cycleOrder = 12, length = 1.31, durationScale = 0.96,
		magOut = 0.08, acquire = 0.23, approach = 0.51, magIn = 0.66, bolt = 0.81,
		screen = cf(-0.020, 0.068, -0.040, 0, 0, 0), work = cf(-0.018, 0.043, -0.020, 5, 16, -10),
		bodyPouch = cf(-0.37, -1.04, -0.10, -44, 20, 12), leftPole = Vector3.new(-0.09, -0.02, 0.11),
		leftShoulder = Vector3.new(-0.05, 0.0, -0.07), rightShoulder = Vector3.new(0.01, 0, -0.02),
		leftArc = Vector3.new(-0.012, 0.024, -0.008), leftLeadMin = 0.018, leftLeadMax = 0.038 },
}

for _, spec in combatEmptySpecs do
	MAGFED[spec.key] = makeCombatEmpty(spec)
end


--------------------------------------------------------------------------------
-- V17 VIDEO-REFERENCE MK18/M4 PASS
--
-- Derived from the three supplied clips. The common doctrine is consistent:
-- butt pad and firing hand remain planted, the rifle rolls only enough to expose
-- the magwell, support elbow leads the hand, magazine indexing happens in the
-- centre workspace, and the bolt action is a separate decisive beat.
--------------------------------------------------------------------------------
local function makeReferenceEmpty(spec: any): any
	local L = spec.length
	local releaseT = L * spec.release
	local pouchT = L * spec.pouch
	local indexT = L * spec.index
	local seatT = L * spec.seat
	local actionT = L * spec.action
	local returnT = L * spec.returnAt
	local rawWork = spec.work
	local rawPitch, rawYaw, rawRoll = rawWork:ToOrientation()
	-- Reference footage keeps the butt pad planted and rolls the receiver toward
	-- the support hand. The old 15-34 degree yaw values swung the muzzle across
	-- the left half of the screen and made every reload look like the same side
	-- presentation. Preserve pitch/roll character, but constrain yaw and bias the
	-- complete weapon back into its normal right-side corridor.
	local work = CFrame.new(
		math.clamp(rawWork.Position.X + 0.040, 0.018, 0.060),
		math.clamp(rawWork.Position.Y, 0.026, 0.086),
		math.clamp(rawWork.Position.Z, -0.034, -0.014)
	) * CFrame.Angles(
		math.clamp(rawPitch, math.rad(-2), math.rad(14)),
		math.clamp(rawYaw * 0.20, math.rad(-5.5), math.rad(5.5)),
		math.clamp(rawRoll * 0.72, math.rad(-15), math.rad(13))
	)
	local settleWork = spec.settleWork or (work * cf(0.001, 0.004, -0.006, -0.45, 0.25, -0.32))
	local bodyPouch = spec.bodyPouch
	local preIndex = spec.preIndex or cf(-0.055, -0.34, 0.11, -11, 4, -7)
	local finalIndex = spec.finalIndex or cf(-0.018, -0.16, 0.035, -4, 1, -2)
	local dropped = spec.drop or cf(0.04, -1.08, 0.10, -46, 7, 12)

	-- V17's poses were the correct base, but the timeline had only a handful of
	-- whole-body poses and used near-instant impact easing on the rifle itself.
	-- These intermediate poses preserve the same workspace while providing the
	-- shoulder unload, elbow lead, contact compression, rebound, and follow-through
	-- that were missing between the original keys.
	local work28 = CFrame.identity:Lerp(work, 0.28)
	local work62 = CFrame.identity:Lerp(work, 0.62)
	local work88 = work:Lerp(settleWork, 0.58)
	local seatCompression = settleWork * cf(0.000, 0.012, -0.014, -1.4, 0.25, -0.65)
	local seatRebound = settleWork * cf(0.001, -0.004, 0.006, 0.55, -0.12, 0.28)
	local actionPrep = settleWork * cf(-0.002, 0.004, -0.004, -0.35, 0.15, -0.18)
	local actionReaction = settleWork * cf(0.001, 0.009, -0.010, -0.95, 0.18, -0.38)
	local returnHalf = CFrame.identity:Lerp(settleWork, 0.48)
	local shoulderCatch = cf(0.004, 0.006, -0.012, 0.45, 1.05, -0.60)

	local left = {
		{ 0.000, "left" },
		{ L * 0.028, { grip = "left", offset = cf(-0.002, 0.008, -0.004, 0.5, 0, -0.6) }, "linear" },
		{ L * 0.060, { grip = "left", offset = cf(-0.006, 0.020, -0.012, 1.6, 0, -2.0) }, "out" },
	}
	if spec.strip then
		table.insert(left, { math.max(releaseT - L * 0.025, L * 0.065), { grip = "magwell", offset = cf(0.00, 0.025, 0.010, -1, 0, 0) }, "linear" })
		table.insert(left, { releaseT, { grip = "magwell", offset = cf(0.00, -0.020, 0.015, 2, 0, 0) }, "contact" })
	end
	-- The hand leaves the pouch in stages: elbow first, magazine second, wrist last.
	table.insert(left, { indexT - L * 0.165, { grip = "magwell", offset = preIndex * cf(-0.018, -0.12, 0.035, -4, 2, -3) }, "linear" })
	table.insert(left, { indexT - L * 0.105, { grip = "magwell", offset = preIndex * cf(-0.008, -0.055, 0.018, -2, 1, -1) }, "linear" })
	table.insert(left, { indexT - L * 0.055, { grip = "magwell", offset = preIndex }, "linear" })
	table.insert(left, { indexT, { grip = "magwell", offset = finalIndex }, "out" })
	-- Lip contact, partial insertion, full seat, rebound.
	table.insert(left, { seatT - L * 0.060, { grip = "magwell", offset = finalIndex * cf(0, -0.075, 0.020, -2.0, 0, 0) }, "linear" })
	table.insert(left, { seatT - L * 0.025, { grip = "magwell", offset = cf(0, -0.026, 0.007, -0.8, 0, 0) }, "contact" })
	table.insert(left, { seatT, "magwell", "contact" })
	table.insert(left, { seatT + L * 0.026, { grip = "magwell", offset = cf(0, 0.018, -0.006, 1.2, 0, 0) }, "out" })
	table.insert(left, { seatT + L * 0.060, "magwell", "out" })
	if spec.pushPull then
		table.insert(left, { seatT + L * 0.082, { grip = "magwell", offset = cf(0, -0.050, 0.014, 2, 0, 0) }, "out" })
		table.insert(left, { seatT + L * 0.118, "magwell", "contact" })
	end
	if spec.tap then
		table.insert(left, { seatT + L * 0.078, { grip = "magwell", offset = cf(0, 0.050, -0.012, -4, 0, 0) }, "out" })
		table.insert(left, { seatT + L * 0.116, "magwell", "contact" })
	end
	if spec.charge then
		table.insert(left, { actionT - L * 0.125, { grip = "charge", offset = cf(-0.018, 0.018, -0.018, -4, 3, -5) }, "linear" })
		table.insert(left, { actionT - L * 0.070, "charge", "out" })
		table.insert(left, { actionT, { grip = "charge", offset = cf(0, 0, 0.15, 0, 0, 0) }, "contact" })
		table.insert(left, { actionT + L * 0.035, { grip = "charge", offset = cf(0, 0.006, 0.035, 1, 0, 0) }, "out" })
		table.insert(left, { actionT + L * 0.065, "charge", "impact" })
	else
		table.insert(left, { actionT - L * 0.100, { grip = "release", offset = cf(-0.025, -0.015, 0.012, 0, 0, -5) }, "linear" })
		table.insert(left, { actionT - L * 0.045, "release", "out" })
		table.insert(left, { actionT, "release", "contact" })
		table.insert(left, { actionT + L * 0.035, { grip = "release", offset = cf(0.012, 0.012, -0.006, -2, 0, 3) }, "out" })
	end
	-- Recovery is an arm chain, not one key that teleports back to the rail.
	table.insert(left, { returnT - L * 0.080, { grip = "left", offset = cf(-0.085, -0.10, 0.055, -7, 3, -8) }, "linear" })
	table.insert(left, { returnT, { grip = "left", offset = cf(-0.030, -0.025, 0.020, -2, 1, -3) }, "out" })
	table.insert(left, { L * 0.965, { grip = "left", offset = cf(0.006, 0.010, -0.008, 0.7, 0, 0.8) }, "out" })
	table.insert(left, { L, "left", "settle" })
	table.sort(left, function(a, b) return a[1] < b[1] end)

	local weapon = {
		{ 0.000, CFrame.identity },
		{ L * 0.025, cf(0, 0.004, -0.003, -0.35, -0.20, 0.25), "linear" },
		{ L * 0.055, cf(0, 0.009, -0.007, -0.8, -0.55, 0.55), "anticipate" },
		{ L * 0.090, work28, "linear" },
		{ L * 0.125, work62, "out" },
		{ L * 0.170, work, "out" },
		{ pouchT, work * cf(0.001, 0.004, -0.003, -0.35, 0.35, -0.22), "linear" },
		{ indexT - L * 0.080, work88, "linear" },
		{ indexT, settleWork, "out" },
		{ seatT - L * 0.055, actionPrep, "linear" },
		{ seatT, seatCompression, "contact" },
		{ seatT + L * 0.030, seatRebound, "out" },
		{ seatT + L * 0.075, settleWork, "out" },
		{ actionT - L * 0.050, actionPrep, "linear" },
		{ actionT, actionReaction, "contact" },
		{ actionT + L * 0.038, seatRebound, "out" },
		{ returnT - L * 0.075, returnHalf, "linear" },
		{ returnT, shoulderCatch, "out" },
		{ L * 0.965, cf(-0.002, -0.003, 0.006, -0.30, -0.55, 0.35), "out" },
		{ L, CFrame.identity, "settle" },
	}
	table.sort(weapon, function(a, b) return a[1] < b[1] end)

	local mag = {
		{ 0.000, CFrame.identity },
		{ releaseT, CFrame.identity, "linear" },
		{ releaseT + L * 0.028, cf(0.006, -0.045, 0.012, -5, 0, 1), "out" },
		{ releaseT + L * 0.065, CFrame.identity:Lerp(dropped, 0.38), "linear" },
		{ releaseT + L * 0.105, dropped, "out" },
		{ indexT - L * 0.165, preIndex * cf(-0.018, -0.12, 0.035, -4, 2, -3), "linear" },
		{ indexT - L * 0.105, preIndex * cf(-0.008, -0.055, 0.018, -2, 1, -1), "linear" },
		{ indexT - L * 0.055, preIndex, "linear" },
		{ indexT, finalIndex, "out" },
		{ seatT - L * 0.055, finalIndex * cf(0, -0.075, 0.020, -2, 0, 0), "linear" },
		{ seatT - L * 0.020, cf(0, -0.024, 0.006, -0.8, 0, 0), "contact" },
		{ seatT, CFrame.identity, "impact" },
		{ seatT + L * 0.026, cf(0, 0.012, -0.004, 0.8, 0, 0), "out" },
		{ seatT + L * 0.060, CFrame.identity, "out" },
		{ L, CFrame.identity, "linear" },
	}
	table.sort(mag, function(a, b) return a[1] < b[1] end)

	local leftShoulderPeak = spec.leftShoulder or Vector3.new(-0.045, 0.010, -0.065)
	local rightShoulderPeak = spec.rightShoulder or Vector3.new(0.008, 0, -0.015)
	local leftPolePeak = spec.leftPole or Vector3.new(-0.09, -0.02, 0.10)
	local rightPolePeak = spec.rightPole or Vector3.new(0.015, -0.005, 0.02)

	local anim = {
		length = L,
		technique = spec.technique,
		cycleOrder = spec.cycleOrder,
		durationScale = spec.durationScale or 1,
		continuousMotion = true,
		humanity = {
			leftLeadMin = spec.leftLeadMin or 0.006,
			leftLeadMax = spec.leftLeadMax or 0.022,
			rightLagMin = 0.004,
			rightLagMax = 0.016,
			cameraLagMin = 0.003,
			cameraLagMax = 0.010,
			leftArc = spec.leftArc or Vector3.new(-0.010, 0.028, -0.005),
			rightArc = Vector3.new(0.001, 0.002, 0),
			microPos = 0,
			microRotDeg = 0,
		},
		screen = {
			{ 0.000, CFrame.identity },
			{ L * 0.050, cf(0.008, 0.006, -0.004, 0, 0, 0), "linear" },
			{ L * 0.100, cf(0.020, 0.014, -0.008, 0, 0, 0), "linear" },
			{ L * 0.165, spec.screen or cf(0.032, 0.022, -0.012, 0, 0, 0), "linear" },
			{ returnT - L * 0.080, spec.screen or cf(0.032, 0.022, -0.012, 0, 0, 0), "linear" },
			{ returnT, cf(0.014, 0.010, -0.006, 0, 0, 0), "linear" },
			{ L * 0.965, cf(0.004, 0.003, -0.002, 0, 0, 0), "linear" },
			{ L, CFrame.identity, "linear" },
		},
		camera = {
			{ 0.000, CFrame.identity },
			{ L * 0.075, cf(-0.0005, -0.0010, 0, 0.02, -0.01, 0.02), "linear" },
			{ L * 0.150, cf(-0.0010, -0.0020, 0, 0.05, -0.03, 0.05), "out" },
			{ seatT, cf(-0.0012, -0.0022, 0.0005, 0.055, -0.025, 0.055), "linear" },
			{ seatT + L * 0.035, cf(0.0004, 0.0006, -0.0010, -0.035, 0.012, -0.030), "out" },
			{ actionT, cf(-0.0010, -0.0018, 0, 0.045, -0.020, 0.045), "linear" },
			{ returnT, cf(0.0005, 0.0008, -0.0012, -0.04, 0.015, -0.025), "out" },
			{ L, CFrame.identity, "settle" },
		},
		weapon = weapon,
		left = left,
		right = {
			{ 0.000, "right" },
			{ L * 0.070, { grip = "right", offset = cf(0.002, -0.003, 0.001, 0.2, -0.25, 0.18) }, "linear" },
			{ releaseT, { grip = "right", offset = cf(0.006, -0.008, 0.003, 0.6, -0.7, 0.5) }, "contact" },
			{ seatT, { grip = "right", offset = cf(0.007, -0.009, 0.004, 0.7, -0.75, 0.55) }, "linear" },
			{ seatT + L * 0.032, { grip = "right", offset = cf(0.003, -0.004, 0.001, 0.25, -0.30, 0.22) }, "out" },
			{ actionT, { grip = "right", offset = cf(0.006, -0.008, 0.003, 0.6, -0.7, 0.5) }, "contact" },
			{ actionT + L * 0.040, { grip = "right", offset = cf(0.002, -0.003, 0.001, 0.18, -0.20, 0.15) }, "out" },
			{ returnT, { grip = "right", offset = cf(-0.002, 0.004, -0.005, -0.4, 0.3, -0.3) }, "out" },
			{ L * 0.965, { grip = "right", offset = cf(0.001, -0.001, 0.002, 0.15, -0.10, 0.12) }, "out" },
			{ L, "right", "settle" },
		},
		mag = mag,
		magHide = {
			{ 0.000, false },
			{ releaseT + L * 0.108, true },
			{ indexT - L * 0.170, false },
		},
		bolt = {
			{ 0.000, 1 },
			{ actionT - L * 0.018, 1, "linear" },
			{ actionT + L * 0.020, 0, "impact" },
		},
		leftBody = {
			{ 0.000, bodyPouch },
			{ pouchT - L * 0.045, bodyPouch * cf(0.010, 0.018, -0.012, 2, -1, 2), "linear" },
			{ pouchT, bodyPouch, "out" },
			{ indexT - L * 0.165, bodyPouch * cf(-0.010, 0.025, -0.010, -2, 1, -2), "linear" },
			{ L, bodyPouch, "linear" },
		},
		leftBodyWeight = {
			{ 0.000, 0 },
			{ L * 0.055, 0 },
			{ L * 0.115, 0.30, "linear" },
			{ L * 0.185, 0.78, "linear" },
			{ L * 0.225, 1, "linear" },
			{ pouchT, 1, "linear" },
			{ indexT - L * 0.205, 1, "linear" },
			{ indexT - L * 0.145, 0.78, "linear" },
			{ indexT - L * 0.085, 0.30, "linear" },
			{ indexT - L * 0.035, 0, "linear" },
			{ L, 0, "linear" },
		},
		leftPole = {
			{ 0.000, Vector3.zero },
			{ L * 0.070, leftPolePeak * 0.20, "linear" },
			{ L * 0.140, leftPolePeak * 0.58, "out" },
			{ pouchT, leftPolePeak, "out" },
			{ indexT, leftPolePeak * 0.82, "linear" },
			{ seatT, leftPolePeak * 0.72, "linear" },
			{ actionT, leftPolePeak * 0.62, "linear" },
			{ returnT, leftPolePeak * 0.25, "out" },
			{ L, Vector3.zero, "settle" },
		},
		rightPole = {
			{ 0.000, Vector3.zero },
			{ L * 0.100, rightPolePeak * 0.45, "out" },
			{ seatT, rightPolePeak, "linear" },
			{ actionT, rightPolePeak * 0.85, "linear" },
			{ returnT, rightPolePeak * 0.30, "out" },
			{ L, Vector3.zero, "settle" },
		},
		leftShoulder = {
			{ 0.000, Vector3.zero },
			{ L * 0.060, leftShoulderPeak * 0.16, "linear" },
			{ L * 0.120, leftShoulderPeak * 0.48, "out" },
			{ L * 0.190, leftShoulderPeak * 0.82, "out" },
			{ pouchT, leftShoulderPeak, "linear" },
			{ indexT, leftShoulderPeak * 0.88, "linear" },
			{ seatT, leftShoulderPeak * 0.82, "linear" },
			{ actionT, leftShoulderPeak * 0.70, "linear" },
			{ returnT, leftShoulderPeak * 0.24, "out" },
			{ L, Vector3.zero, "settle" },
		},
		rightShoulder = {
			{ 0.000, Vector3.zero },
			{ L * 0.085, rightShoulderPeak * 0.35, "linear" },
			{ L * 0.160, rightShoulderPeak, "out" },
			{ seatT, rightShoulderPeak * 0.92, "linear" },
			{ actionT, rightShoulderPeak, "linear" },
			{ returnT, rightShoulderPeak * 0.28, "out" },
			{ L, Vector3.zero, "settle" },
		},
	}
	if spec.charge then
		anim.charge = {
			{ 0.000, 0 },
			{ actionT - L * 0.070, 0, "linear" },
			{ actionT, 0.90, "out" },
			{ actionT + L * 0.030, 1.00, "contact" },
			{ actionT + L * 0.065, 0, "impact" },
		}
	end
	return anim
end

local referenceEmptySpecs = {
	{ key="reload_empty", technique="ref_operator_compact_paddle", cycleOrder=1, length=1.48, release=.10, pouch=.28, index=.56, seat=.70, action=.84, returnAt=.91,
		work=cf(-.018,.040,-.022,5,15,-10), bodyPouch=cf(-.36,-1.02,-.10,-43,18,11), leftArc=Vector3.new(-.012,.030,-.006) },
	{ key="reload_empty_b", technique="ref_mwii_high_charge", cycleOrder=2, length=1.72, release=.10, pouch=.30, index=.58, seat=.71, action=.84, returnAt=.92, charge=true,
		work=cf(-.070,.095,-.030,13,30,-22), bodyPouch=cf(-.43,-1.00,-.08,-46,22,14), leftArc=Vector3.new(-.025,.055,-.010), preIndex=cf(-.09,-.37,.13,-15,7,-10) },
	{ key="reload_empty_c", technique="ref_mk18_support_roll", cycleOrder=3, length=1.50, release=.09, pouch=.27, index=.55, seat=.69, action=.84, returnAt=.91,
		work=cf(.025,.045,-.025,4,-17,13), bodyPouch=cf(-.31,-1.08,-.12,-42,14,7), leftArc=Vector3.new(.020,.032,-.004) },
	{ key="reload_empty_d", technique="ref_centerline_paddle", cycleOrder=4, length=1.43, release=.09, pouch=.25, index=.53, seat=.68, action=.82, returnAt=.90,
		work=cf(-.004,.055,-.020,7,5,-4), bodyPouch=cf(-.34,-1.03,-.09,-44,17,10), leftArc=Vector3.new(-.006,.025,-.006) },
	{ key="reload_empty_e", technique="ref_inboard_chest", cycleOrder=5, length=1.57, release=.10, pouch=.29, index=.57, seat=.71, action=.85, returnAt=.92,
		work=cf(-.085,.055,-.023,8,25,-18), bodyPouch=cf(-.47,-.96,-.06,-47,25,16), leftArc=Vector3.new(-.030,.040,-.010) },
	{ key="reload_empty_f", technique="ref_outboard_index", cycleOrder=6, length=1.54, release=.10, pouch=.28, index=.57, seat=.71, action=.85, returnAt=.92,
		work=cf(.060,.055,-.027,6,-22,16), bodyPouch=cf(-.28,-1.10,-.13,-40,10,4), leftArc=Vector3.new(.035,.036,-.004) },
	{ key="reload_empty_g", technique="ref_low_workspace", cycleOrder=7, length=1.50, release=.09, pouch=.26, index=.55, seat=.70, action=.84, returnAt=.91,
		work=cf(-.035,-.010,-.016,2,17,-13), bodyPouch=cf(-.35,-1.15,-.12,-48,17,9), leftArc=Vector3.new(-.012,.015,-.004) },
	{ key="reload_empty_h", technique="ref_muzzle_up_charge", cycleOrder=8, length=1.78, release=.11, pouch=.31, index=.59, seat=.72, action=.85, returnAt=.93, charge=true,
		work=cf(-.055,.095,-.024,17,24,-17), bodyPouch=cf(-.41,-1.00,-.08,-45,21,13), leftArc=Vector3.new(-.020,.060,-.010) },
	{ key="reload_empty_i", technique="ref_push_pull", cycleOrder=9, length=1.61, release=.10, pouch=.28, index=.55, seat=.68, action=.86, returnAt=.93, pushPull=true,
		work=cf(-.020,.050,-.023,6,18,-12), bodyPouch=cf(-.37,-1.04,-.10,-44,18,11), leftArc=Vector3.new(-.010,.034,-.006) },
	{ key="reload_empty_j", technique="ref_tactile_baseplate", cycleOrder=10, length=1.65, release=.10, pouch=.29, index=.56, seat=.69, action=.86, returnAt=.93, tap=true,
		work=cf(-.030,.058,-.024,7,20,-14), bodyPouch=cf(-.39,-1.02,-.09,-44,20,12), leftArc=Vector3.new(-.014,.040,-.006) },
	{ key="reload_empty_k", technique="ref_fast_emergency", cycleOrder=11, length=1.30, release=.075, pouch=.22, index=.50, seat=.65, action=.79, returnAt=.88,
		work=cf(-.010,.036,-.018,4,13,-9), bodyPouch=cf(-.33,-1.03,-.10,-42,16,9), leftLeadMin=.014, leftLeadMax=.032, leftArc=Vector3.new(-.006,.024,-.004) },
	{ key="reload_empty_l", technique="ref_wide_pouch_draw", cycleOrder=12, length=1.62, release=.10, pouch=.30, index=.58, seat=.72, action=.86, returnAt=.93,
		work=cf(-.025,.050,-.025,5,12,-8), bodyPouch=cf(-.58,-.94,-.02,-48,34,22), leftArc=Vector3.new(-.055,.045,-.008), preIndex=cf(-.12,-.35,.13,-13,9,-12) },
	{ key="reload_empty_m", technique="ref_palm_bolt_release", cycleOrder=13, length=1.55, release=.09, pouch=.27, index=.55, seat=.69, action=.83, returnAt=.91,
		work=cf(-.045,.060,-.022,7,22,-16), bodyPouch=cf(-.38,-1.04,-.10,-45,19,12), leftPole=Vector3.new(-.14,.01,.13), leftArc=Vector3.new(-.020,.035,-.006) },
	{ key="reload_empty_n", technique="ref_overhand_charge", cycleOrder=14, length=1.82, release=.10, pouch=.30, index=.58, seat=.71, action=.84, returnAt=.93, charge=true,
		work=cf(-.095,.080,-.030,11,34,-25), bodyPouch=cf(-.45,-1.00,-.07,-47,24,15), leftPole=Vector3.new(-.18,.04,.15), leftArc=Vector3.new(-.035,.060,-.012) },
}
for _, spec in referenceEmptySpecs do
	MAGFED[spec.key] = makeReferenceEmpty(spec)
end


--------------------------------------------------------------------------------
-- V23 SUBTLE FORWARD / COMPLETE-STOCK RELOAD OVERRIDE
--
-- The previous pass still exposed the stock because it tried to create variety
-- by rotating the complete rifle. Real variation comes from the support-hand
-- route, workspace height, magazine index, and bolt manipulation while the
-- firing hand and shoulder pocket remain stable. These tracks therefore keep
-- yaw almost zero, use modest bore-axis roll, and hide rear furniture while the
-- action is active -- the same first-person cheat used by production shooters.
--------------------------------------------------------------------------------
local function operatorReloadV22(spec: any): any
	local L = spec.length
	local releaseT = L * spec.release
	local pouchT = L * spec.pouch
	local indexT = L * spec.index
	local seatT = L * spec.seat
	local actionT = L * spec.action
	local returnT = L * spec.returnAt
	local rawWork = spec.work
	local rawPitch, rawYaw, rawRoll = rawWork:ToOrientation()
	-- The rifle stays shouldered.  Only a small forward/up presentation is used
	-- so the hands and magazine can be read; the weapon is not turned sideways.
	local work = CFrame.new(
		math.clamp(rawWork.Position.X, -0.006, 0.012),
		math.clamp(rawWork.Position.Y, 0.012, 0.045),
		math.clamp(rawWork.Position.Z, -0.018, -0.006)
	) * CFrame.Angles(
		math.clamp(rawPitch, math.rad(-1.5), math.rad(6.0)),
		math.clamp(rawYaw, math.rad(-1.25), math.rad(1.25)),
		math.clamp(rawRoll, math.rad(-4.0), math.rad(4.0))
	)
	local oldScreen = spec.screen or CFrame.identity
	local screenWork = cf(
		0,
		math.clamp(oldScreen.Position.Y + 0.026, 0.038, 0.058),
		-math.clamp(math.abs(oldScreen.Position.Z) + 0.105, 0.112, 0.136),
		0, 0, 0
	)
	local bodyPouch = spec.bodyPouch
	local preIndex = spec.preIndex or cf(-0.055, -0.315, 0.105, -10, 3, -6)
	local finalIndex = spec.finalIndex or cf(-0.012, -0.135, 0.028, -3, 0, -1)
	local drop = spec.drop or cf(0.035, -1.02, 0.10, -42, 7, 10)

	local work25 = CFrame.identity:Lerp(work, 0.25)
	local work58 = CFrame.identity:Lerp(work, 0.58)
	local work86 = CFrame.identity:Lerp(work, 0.86)
	local compression = work * cf(0, 0.010, -0.012, -1.0, 0.15, -0.45)
	local rebound = work * cf(0.001, -0.004, 0.006, 0.45, -0.08, 0.22)

	local left = {
		{0, "left"},
		{L*.035, {grip="left", offset=cf(-.002,.010,-.004,.5,0,-.7)}, "linear"},
		{L*.075, {grip="left", offset=cf(-.008,.030,-.014,1.8,0,-2.4)}, "out"},
	}
	if spec.strip then
		table.insert(left, {releaseT-L*.025, {grip="magwell", offset=cf(0,.035,.012,-1,0,0)}, "linear"})
		table.insert(left, {releaseT, "magwell", "contact"})
	end
	-- The pouch section is body-relative through leftBodyWeight. These weapon-
	-- local keys only guide the transition into and out of that body workspace.
	table.insert(left, {pouchT-L*.060, {grip="magwell", offset=preIndex*cf(-.030,-.22,.070,-6,2,-4)}, "linear"})
	table.insert(left, {indexT-L*.155, {grip="magwell", offset=preIndex*cf(-.024,-.15,.050,-5,2,-3)}, "linear"})
	table.insert(left, {indexT-L*.095, {grip="magwell", offset=preIndex*cf(-.010,-.075,.025,-3,1,-2)}, "linear"})
	table.insert(left, {indexT-L*.040, {grip="magwell", offset=preIndex}, "linear"})
	table.insert(left, {indexT, {grip="magwell", offset=finalIndex}, "out"})
	-- Magwell lip contact -> partial insertion -> seat -> hand compression.
	table.insert(left, {seatT-L*.055, {grip="magwell", offset=finalIndex*cf(0,-.068,.018,-1.6,0,0)}, "linear"})
	table.insert(left, {seatT-L*.020, {grip="magwell", offset=cf(0,-.024,.006,-.7,0,0)}, "contact"})
	table.insert(left, {seatT, "magwell", "contact"})
	table.insert(left, {seatT+L*.028, {grip="magwell", offset=cf(0,.016,-.005,1.0,0,0)}, "out"})
	table.insert(left, {seatT+L*.060, "magwell", "out"})
	if spec.pushPull then
		table.insert(left, {seatT+L*.080, {grip="magwell", offset=cf(0,-.045,.012,1.8,0,0)}, "out"})
		table.insert(left, {seatT+L*.118, "magwell", "contact"})
	elseif spec.tap then
		table.insert(left, {seatT+L*.078, {grip="magwell", offset=cf(0,.045,-.010,-3.2,0,0)}, "out"})
		table.insert(left, {seatT+L*.112, "magwell", "contact"})
	end
	if spec.charge then
		table.insert(left, {actionT-L*.125, {grip="charge", offset=cf(-.016,.018,-.020,-4,2,-5)}, "linear"})
		table.insert(left, {actionT-L*.070, "charge", "out"})
		table.insert(left, {actionT, {grip="charge", offset=cf(0,0,.145,0,0,0)}, "contact"})
		table.insert(left, {actionT+L*.035, {grip="charge", offset=cf(0,.006,.030,1,0,0)}, "out"})
		table.insert(left, {actionT+L*.065, "charge", "impact"})
	else
		table.insert(left, {actionT-L*.100, {grip="release", offset=cf(-.022,-.012,.010,0,0,-4)}, "linear"})
		table.insert(left, {actionT-L*.045, "release", "out"})
		table.insert(left, {actionT, "release", "contact"})
		table.insert(left, {actionT+L*.035, {grip="release", offset=cf(.010,.010,-.005,-2,0,2)}, "out"})
	end
	-- Recovery has three stages; the arm never teleports to the handguard.
	table.insert(left, {returnT-L*.090, {grip="left", offset=cf(-.105,-.120,.065,-8,3,-9)}, "linear"})
	table.insert(left, {returnT-L*.040, {grip="left", offset=cf(-.060,-.060,.040,-5,2,-6)}, "linear"})
	table.insert(left, {returnT, {grip="left", offset=cf(-.022,-.018,.016,-2,1,-2)}, "out"})
	table.insert(left, {L*.965, {grip="left", offset=cf(.005,.008,-.006,.6,0,.7)}, "out"})
	table.insert(left, {L, "left", "settle"})
	table.sort(left, function(a,b) return a[1] < b[1] end)

	local weapon = {
		{0, CFrame.identity},
		{L*.030, cf(0,.003,-.002,-.25,-.08,.16), "linear"},
		{L*.065, cf(0,.008,-.006,-.65,-.18,.45), "anticipate"},
		{L*.105, work25, "linear"},
		{L*.145, work58, "out"},
		{L*.190, work86, "out"},
		{pouchT, work, "linear"},
		{indexT-L*.080, work*cf(.001,.003,-.003,-.25,.10,-.15), "linear"},
		{indexT, work, "out"},
		{seatT-L*.050, work, "linear"},
		{seatT, compression, "contact"},
		{seatT+L*.030, rebound, "out"},
		{seatT+L*.070, work, "out"},
		{actionT-L*.050, work, "linear"},
		{actionT, compression*cf(0,.002,-.004,-.30,.05,-.12), "contact"},
		{actionT+L*.040, rebound, "out"},
		{returnT-L*.085, CFrame.identity:Lerp(work, .55), "linear"},
		{returnT-L*.035, CFrame.identity:Lerp(work, .24), "linear"},
		{returnT, cf(.002,.004,-.007,.30,.35,-.28), "out"},
		{L*.965, cf(-.001,-.002,.004,-.20,-.30,.20), "out"},
		{L, CFrame.identity, "settle"},
	}

	local mag = {
		{0,CFrame.identity}, {releaseT,CFrame.identity,"linear"},
		{releaseT+L*.030,cf(.006,-.050,.012,-5,0,1),"out"},
		{releaseT+L*.070,CFrame.identity:Lerp(drop,.42),"linear"},
		{releaseT+L*.112,drop,"out"},
		{indexT-L*.155,preIndex*cf(-.024,-.15,.050,-5,2,-3),"linear"},
		{indexT-L*.095,preIndex*cf(-.010,-.075,.025,-3,1,-2),"linear"},
		{indexT-L*.040,preIndex,"linear"},
		{indexT,finalIndex,"out"},
		{seatT-L*.055,finalIndex*cf(0,-.068,.018,-1.6,0,0),"linear"},
		{seatT-L*.020,cf(0,-.024,.006,-.7,0,0),"contact"},
		{seatT,CFrame.identity,"impact"},
		{seatT+L*.030,cf(0,.010,-.003,.7,0,0),"out"},
		{seatT+L*.060,CFrame.identity,"out"}, {L,CFrame.identity,"linear"},
	}

	local lShoulder = spec.leftShoulder or Vector3.new(-.040,.008,-.055)
	local rShoulder = spec.rightShoulder or Vector3.new(.006,0,-.012)
	local lPole = spec.leftPole or Vector3.new(-.085,-.015,.095)
	local rPole = Vector3.new(.012,-.004,.018)

	local anim = {
		length=L, technique=spec.technique, cycleOrder=spec.cycleOrder,
		continuousMotion=true, hideRear=false, cameraSafeScale=.92,
		smoothing={weapon=14,screen=11,camera=15,left=18,right=20,body=15,shoulder=14,pole=16,contact=40},
		contactWindows={{seatT-L*.030,seatT+L*.045},{actionT-L*.020,actionT+L*.045}},
		humanity={leftLeadMin=.004,leftLeadMax=.014,rightLagMin=.003,rightLagMax=.010,cameraLagMin=.002,cameraLagMax=.007,leftArc=spec.leftArc or Vector3.new(-.008,.024,-.004),rightArc=Vector3.new(.001,.002,0),microPos=0,microRotDeg=0},
		screen={
			{0,CFrame.identity},
			{L*.045,cf(0,.008,-.022,0,0,0),"linear"},
			{L*.105,screenWork:Lerp(CFrame.identity,.38),"out"},
			{L*.165,screenWork,"out"},
			{seatT-L*.030,screenWork,"linear"},
			{seatT+L*.040,screenWork*cf(0,.004,-.006,0,0,0),"out"},
			{actionT+L*.045,screenWork,"linear"},
			{returnT-L*.055,screenWork:Lerp(CFrame.identity,.18),"linear"},
			{returnT,screenWork:Lerp(CFrame.identity,.58),"out"},
			{L*.970,cf(0,.003,-.010,0,0,0),"out"},
			{L,CFrame.identity,"settle"},
		},
		camera={
			{0,CFrame.identity},{L*.12,cf(-.0008,-.0014,0,.035,-.015,.035),"out"},
			{seatT,cf(-.001,-.0018,.0004,.045,-.018,.045),"linear"},
			{seatT+L*.035,cf(.0003,.0005,-.0008,-.025,.010,-.020),"out"},
			{actionT,cf(-.0008,-.0014,0,.035,-.015,.035),"linear"},
			{returnT,cf(.0003,.0005,-.0008,-.028,.010,-.018),"out"},{L,CFrame.identity,"settle"},
		},
		weapon=weapon, left=left,
		right={
			{0,"right"},{L*.08,{grip="right",offset=cf(.002,-.003,.001,.2,-.2,.15)},"linear"},
			{seatT,{grip="right",offset=cf(.005,-.007,.003,.5,-.55,.40)},"linear"},
			{seatT+L*.035,{grip="right",offset=cf(.002,-.003,.001,.2,-.2,.15)},"out"},
			{actionT,{grip="right",offset=cf(.005,-.007,.003,.5,-.55,.40)},"contact"},
			{returnT,{grip="right",offset=cf(-.001,.003,-.004,-.3,.2,-.2)},"out"},{L,"right","settle"},
		},
		mag=mag,
		magHide={{0,false},{releaseT+L*.115,true},{indexT-L*.160,false}},
		bolt={{0,1},{actionT-L*.018,1,"linear"},{actionT+L*.020,0,"impact"}},
		leftBody={
			{0,bodyPouch},{pouchT-L*.055,bodyPouch*cf(.010,.018,-.010,2,-1,2),"linear"},
			{pouchT,bodyPouch,"out"},{indexT-L*.165,bodyPouch*cf(-.010,.022,-.008,-2,1,-2),"linear"},{L,bodyPouch,"linear"},
		},
		leftBodyWeight={
			{0,0},{L*.075,0},{L*.145,.34,"linear"},{L*.205,.82,"linear"},{L*.245,1,"linear"},
			{pouchT,1,"linear"},{indexT-L*.205,1,"linear"},{indexT-L*.145,.76,"linear"},
			{indexT-L*.085,.28,"linear"},{indexT-L*.035,0,"linear"},{L,0,"linear"},
		},
		leftPole={{0,Vector3.zero},{L*.12,lPole*.52,"out"},{pouchT,lPole,"out"},{seatT,lPole*.68,"linear"},{actionT,lPole*.58,"linear"},{returnT,lPole*.20,"out"},{L,Vector3.zero,"settle"}},
		rightPole={{0,Vector3.zero},{L*.15,rPole,"out"},{actionT,rPole*.85,"linear"},{returnT,rPole*.24,"out"},{L,Vector3.zero,"settle"}},
		leftShoulder={{0,Vector3.zero},{L*.11,lShoulder*.48,"out"},{L*.20,lShoulder,"out"},{pouchT,lShoulder,"linear"},{seatT,lShoulder*.76,"linear"},{actionT,lShoulder*.64,"linear"},{returnT,lShoulder*.20,"out"},{L,Vector3.zero,"settle"}},
		rightShoulder={{0,Vector3.zero},{L*.16,rShoulder,"out"},{actionT,rShoulder,"linear"},{returnT,rShoulder*.22,"out"},{L,Vector3.zero,"settle"}},
	}
	if spec.charge then
		anim.charge={{0,0},{actionT-L*.070,0,"linear"},{actionT,.90,"out"},{actionT+L*.030,1,"contact"},{actionT+L*.065,0,"impact"}}
	end
	return anim
end

-- Variety is in human technique, not in throwing the rifle across the screen.
-- All work poses keep yaw inside roughly two degrees; roll and workspace height
-- provide the visual distinction while the stock remains in the shoulder.
local operatorReloadSpecsV22 = {
	{key="reload_empty",technique="v22_compact_paddle",cycleOrder=1,length=1.46,release=.095,pouch=.28,index=.555,seat=.70,action=.84,returnAt=.92,work=cf(.018,.042,-.025,5,1.5,-8),screen=cf(.008,.016,-.008,0,0,0),bodyPouch=cf(-.35,-1.02,-.10,-43,18,11)},
	{key="reload_empty_b",technique="v22_high_charge",cycleOrder=2,length=1.70,release=.10,pouch=.30,index=.58,seat=.715,action=.85,returnAt=.93,charge=true,work=cf(.010,.080,-.030,10,1,-10),screen=cf(.006,.026,-.012,0,0,0),bodyPouch=cf(-.43,-1.00,-.08,-46,22,14),leftArc=Vector3.new(-.020,.045,-.008)},
	{key="reload_empty_c",technique="v22_support_roll",cycleOrder=3,length=1.50,release=.09,pouch=.27,index=.55,seat=.69,action=.84,returnAt=.92,work=cf(.025,.048,-.027,4,-1.5,10),screen=cf(.012,.018,-.009,0,0,0),bodyPouch=cf(-.31,-1.08,-.12,-42,14,7),leftArc=Vector3.new(.016,.030,-.004)},
	{key="reload_empty_d",technique="v22_centerline_paddle",cycleOrder=4,length=1.42,release=.09,pouch=.25,index=.53,seat=.68,action=.82,returnAt=.91,work=cf(.012,.052,-.024,6,.5,-5),screen=cf(.006,.014,-.007,0,0,0),bodyPouch=cf(-.34,-1.03,-.09,-44,17,10)},
	{key="reload_empty_e",technique="v22_inboard_chest",cycleOrder=5,length=1.56,release=.10,pouch=.29,index=.57,seat=.71,action=.85,returnAt=.93,work=cf(.006,.060,-.028,7,1.8,-11),screen=cf(.004,.020,-.010,0,0,0),bodyPouch=cf(-.46,-.96,-.06,-47,25,16),leftArc=Vector3.new(-.025,.038,-.008)},
	{key="reload_empty_f",technique="v22_outboard_index",cycleOrder=6,length=1.53,release=.10,pouch=.28,index=.57,seat=.71,action=.85,returnAt=.93,work=cf(.030,.055,-.029,6,-1.8,9),screen=cf(.015,.018,-.010,0,0,0),bodyPouch=cf(-.28,-1.10,-.13,-40,10,4),leftArc=Vector3.new(.030,.034,-.004)},
	{key="reload_empty_g",technique="v22_low_workspace",cycleOrder=7,length=1.49,release=.09,pouch=.26,index=.55,seat=.70,action=.84,returnAt=.92,work=cf(.015,.022,-.020,2,1,-7),screen=cf(.008,.008,-.006,0,0,0),bodyPouch=cf(-.35,-1.15,-.12,-48,17,9)},
	{key="reload_empty_h",technique="v22_muzzle_up_charge",cycleOrder=8,length=1.76,release=.11,pouch=.31,index=.59,seat=.72,action=.85,returnAt=.94,charge=true,work=cf(.010,.095,-.028,13,1.5,-11),screen=cf(.006,.030,-.012,0,0,0),bodyPouch=cf(-.41,-1.00,-.08,-45,21,13),leftArc=Vector3.new(-.018,.052,-.009)},
	{key="reload_empty_i",technique="v22_push_pull",cycleOrder=9,length=1.60,release=.10,pouch=.28,index=.55,seat=.68,action=.86,returnAt=.94,pushPull=true,work=cf(.016,.050,-.026,6,1,-8),screen=cf(.008,.018,-.009,0,0,0),bodyPouch=cf(-.37,-1.04,-.10,-44,18,11)},
	{key="reload_empty_j",technique="v22_baseplate_check",cycleOrder=10,length=1.64,release=.10,pouch=.29,index=.56,seat=.69,action=.86,returnAt=.94,tap=true,work=cf(.014,.058,-.027,7,1.2,-9),screen=cf(.008,.020,-.010,0,0,0),bodyPouch=cf(-.39,-1.02,-.09,-44,20,12)},
	{key="reload_empty_k",technique="v22_fast_emergency",cycleOrder=11,length=1.30,release=.075,pouch=.22,index=.50,seat=.65,action=.79,returnAt=.89,work=cf(.020,.038,-.022,4,.8,-7),screen=cf(.010,.014,-.007,0,0,0),bodyPouch=cf(-.33,-1.03,-.10,-42,16,9),leftLeadMin=.010,leftLeadMax=.022},
	{key="reload_empty_l",technique="v22_wide_pouch",cycleOrder=12,length=1.61,release=.10,pouch=.30,index=.58,seat=.72,action=.86,returnAt=.94,work=cf(.014,.052,-.027,5,.6,-7),screen=cf(.007,.019,-.010,0,0,0),bodyPouch=cf(-.56,-.94,-.02,-48,34,22),leftArc=Vector3.new(-.045,.042,-.007),preIndex=cf(-.105,-.33,.12,-12,8,-10)},
	{key="reload_empty_m",technique="v22_palm_release",cycleOrder=13,length=1.54,release=.09,pouch=.27,index=.55,seat=.69,action=.83,returnAt=.92,work=cf(.010,.060,-.025,7,1.5,-10),screen=cf(.006,.020,-.009,0,0,0),bodyPouch=cf(-.38,-1.04,-.10,-45,19,12),leftPole=Vector3.new(-.13,.01,.12)},
	{key="reload_empty_n",technique="v22_overhand_charge",cycleOrder=14,length=1.80,release=.10,pouch=.30,index=.58,seat=.71,action=.84,returnAt=.94,charge=true,work=cf(.006,.078,-.030,10,1.7,-11),screen=cf(.004,.026,-.012,0,0,0),bodyPouch=cf(-.45,-1.00,-.07,-47,24,15),leftPole=Vector3.new(-.17,.04,.14),leftArc=Vector3.new(-.030,.052,-.010)},
}
for _, spec in operatorReloadSpecsV22 do
	MAGFED[spec.key] = operatorReloadV22(spec)
end

-- V23 universal reload presentation.  Every rifle reload keeps the complete
-- buttstock and receiver.  The rifle only comes slightly forward/up; technique
-- variety is carried by the support hand, magazine route, elbow and bolt work.
local function compactReloadCF(value: any): any
	if typeof(value) ~= "CFrame" then
		return value
	end
	local pitch, yaw, roll = value:ToOrientation()
	local p = value.Position
	return CFrame.new(
		math.clamp(p.X, -0.010, 0.014),
		math.clamp(p.Y, -0.010, 0.052),
		math.clamp(p.Z, -0.024, 0.010)
	) * CFrame.Angles(
		math.clamp(pitch, math.rad(-2.0), math.rad(7.0)),
		math.clamp(yaw, math.rad(-1.5), math.rad(1.5)),
		math.clamp(roll, math.rad(-4.5), math.rad(4.5))
	)
end

local function compactReloadTrack(track: any): any
	if not track then return track end
	for _, keyframe in track do
		keyframe[2] = compactReloadCF(keyframe[2])
	end
	return track
end

local function subtleReloadScreen(length: number, order: number): any
	local lane = (math.max(order, 1) - 1) % 3
	local lift = 0.043 + lane * 0.004
	local forward = 0.116 + ((order + 1) % 3) * 0.006
	local work = cf(0, lift, -forward, 0, 0, 0)
	return {
		{0, CFrame.identity},
		{length*.045, cf(0,.007,-.020,0,0,0), "linear"},
		{length*.115, work:Lerp(CFrame.identity,.40), "out"},
		{length*.175, work, "out"},
		{length*.790, work, "linear"},
		{length*.870, work:Lerp(CFrame.identity,.38), "out"},
		{length*.955, cf(0,.003,-.010,0,0,0), "out"},
		{length, CFrame.identity, "settle"},
	}
end

for key, action in MAGFED do
	if type(key) == "string" and key:find("^reload_") and type(action) == "table" then
		action.weapon = compactReloadTrack(action.weapon)
		action.screen = subtleReloadScreen(action.length or 1.5, action.cycleOrder or 1)
		action.continuousMotion = true
		action.hideRear = false
		action.cameraSafeScale = .92
		action.smoothing = {weapon=14,screen=11,camera=15,left=18,right=20,body=15,shoulder=14,pole=16,contact=40}
		action.humanity = action.humanity or {}
		action.humanity.microPos = 0
		action.humanity.microRotDeg = 0
	end
end

--------------------------------------------------------------------------------
-- V24 FIVE CARRIER-RETENTION RELOADS
--
-- These are the only live-magazine rifle reloads. Every technique removes the
-- partial magazine first, physically carries it back to the plate-carrier
-- pouch, then draws a fresh magazine. Each cycle contains one controlled human
-- error, but the operator always recovers without throwing live ammunition.
--------------------------------------------------------------------------------
local function addSortedKey(track: any, key: any)
	table.insert(track, key)
	table.sort(track, function(a, b)
		return a[1] < b[1]
	end)
end

local retentionSpecsV24 = {
	{
		key="reload_tactical", technique="retain_shaky_index", cycleOrder=1,
		length=1.88, magOut=.19, acquire=.51, magIn=.80, durationScale=1.06,
		screen=cf(0,.05,-.13,0,0,0), work=cf(.006,.035,-.018,4,.8,-3),
		bodyPouch=cf(-.34,-1.05,-.10,-43,18,11),
		leftShoulder=Vector3.new(-.065,-.015,-.085), rightShoulder=Vector3.new(.014,-.008,-.030),
		humanity={leftLeadMin=.002,leftLeadMax=.016,leftArc=Vector3.new(-.010,.018,-.006)},
		mistake="shaky_index",
	},
	{
		key="reload_tactical_b", technique="retain_front_lip", cycleOrder=2,
		length=2.02, magOut=.19, acquire=.50, contact=.69, magIn=.84, durationScale=1.12,
		screen=cf(0,.05,-.13,0,0,0), work=cf(.008,.040,-.019,5,.6,-3.5),
		bodyPouch=cf(-.37,-1.03,-.09,-44,20,12), miss=true, missX=.060,
		leftShoulder=Vector3.new(-.070,-.010,-.090), rightShoulder=Vector3.new(.015,-.006,-.032),
		humanity={leftLeadMin=.004,leftLeadMax=.018,leftArc=Vector3.new(.008,.020,-.007)},
		mistake="front_lip_contact",
	},
	{
		key="reload_tactical_c", technique="retain_rear_lip", cycleOrder=3,
		length=2.00, magOut=.20, acquire=.51, contact=.70, magIn=.84, durationScale=1.11,
		screen=cf(0,.052,-.132,0,0,0), work=cf(.004,.038,-.020,4.5,1.0,-2.5),
		bodyPouch=cf(-.31,-1.08,-.11,-45,16,9), miss=true, missX=-.052,
		leftShoulder=Vector3.new(-.060,-.020,-.088), rightShoulder=Vector3.new(.014,-.008,-.030),
		humanity={leftLeadMin=.001,leftLeadMax=.015,leftArc=Vector3.new(-.015,.016,-.008)},
		mistake="rear_lip_contact",
	},
	{
		key="reload_tactical_d", technique="retain_pouch_hesitation", cycleOrder=4,
		length=2.12, magOut=.19, acquire=.59, magIn=.85, durationScale=1.16,
		screen=cf(0,.048,-.128,0,0,0), work=cf(.006,.032,-.018,3.5,.6,-3),
		bodyPouch=cf(-.40,-1.02,-.08,-46,23,14),
		leftShoulder=Vector3.new(-.082,-.045,-.095), rightShoulder=Vector3.new(.012,-.010,-.028),
		humanity={leftLeadMin=.004,leftLeadMax=.020,leftArc=Vector3.new(-.024,-.006,-.010)},
		mistake="pouch_hesitation",
	},
	{
		key="reload_tactical_e", technique="retain_uncertain_seat", cycleOrder=5,
		length=2.04, magOut=.19, acquire=.50, magIn=.79, tap=true, durationScale=1.12,
		screen=cf(0,.052,-.132,0,0,0), work=cf(.005,.040,-.019,5,.8,-3.5),
		bodyPouch=cf(-.35,-1.04,-.10,-44,19,12),
		leftShoulder=Vector3.new(-.068,-.012,-.090), rightShoulder=Vector3.new(.015,-.006,-.032),
		humanity={leftLeadMin=.002,leftLeadMax=.017,leftArc=Vector3.new(.004,.022,-.006)},
		mistake="uncertain_seat_check",
	},
}

-- Remove every earlier tactical suffix so the cycle is exactly these five.
local disabledTacticalKeysV24 = {
	"reload_tactical_f", "reload_tactical_g", "reload_tactical_h",
	"reload_tactical_i", "reload_tactical_j", "reload_tactical_k",
	"reload_tactical_l", "reload_tactical_m", "reload_tactical_n",
}
for _, oldKey in ipairs(disabledTacticalKeysV24) do
	if MAGFED[oldKey] then
		MAGFED[oldKey].disabled = true
	end
end

for _, spec in ipairs(retentionSpecsV24) do
	local action = makeOldFirstTac(spec)
	local L = action.length
	local seatT = spec.magIn * L
	local acquireT = spec.acquire * L

	if spec.mistake == "shaky_index" then
		-- The fresh magazine wavers twice while the support hand searches for the
		-- well, then settles onto the correct insertion axis.
		local shakyLeftKeys = {
			{seatT-L*.150,{grip="magwell",offset=cf(-.048,-.245,.075,-7,2,-5)},"out"},
			{seatT-L*.115,{grip="magwell",offset=cf(-.022,-.215,.064,-5,-1,3)},"linear"},
			{seatT-L*.080,{grip="magwell",offset=cf(-.041,-.175,.050,-6,1,-3)},"linear"},
			{seatT-L*.045,{grip="magwell",offset=cf(-.014,-.105,.028,-3,0,-1)},"out"},
		}
		for _, key in ipairs(shakyLeftKeys) do
			addSortedKey(action.left, key)
		end
		local shakyMagKeys = {
			{seatT-L*.150,cf(-.048,-.245,.075,-7,2,-5),"out"},
			{seatT-L*.115,cf(-.022,-.215,.064,-5,-1,3),"linear"},
			{seatT-L*.080,cf(-.041,-.175,.050,-6,1,-3),"linear"},
			{seatT-L*.045,cf(-.014,-.105,.028,-3,0,-1),"out"},
		}
		for _, key in ipairs(shakyMagKeys) do
			addSortedKey(action.mag, key)
		end
	elseif spec.mistake == "pouch_hesitation" then
		-- The hand catches the carrier lip and makes two short corrections before
		-- the fresh magazine clears the pouch.
		local pouch = spec.bodyPouch
		local pouchKeys = {
			{acquireT-L*.145,pouch*cf(-.018,.010,-.008,-2,2,-3),"out"},
			{acquireT-L*.105,pouch*cf(.012,-.006,.004,2,-1,2),"linear"},
			{acquireT-L*.065,pouch*cf(-.008,.014,-.006,-1,1,-2),"linear"},
			{acquireT,pouch,"out"},
		}
		for _, key in ipairs(pouchKeys) do
			addSortedKey(action.leftBody, key)
		end
	elseif spec.mistake == "uncertain_seat_check" then
		-- The magazine seats, is pulled down slightly for a tactile verification,
		-- then receives a positive baseplate press before the hand returns.
		local seatLeftKeys = {
			{seatT+L*.030,{grip="magwell",offset=cf(0,-.030,.008,2,0,0)},"out"},
			{seatT+L*.070,{grip="magwell",offset=cf(0,.010,-.006,-1,0,0)},"contact"},
			{seatT+L*.115,{grip="magwell",offset=cf(0,.038,-.018,-3,0,0)},"impact"},
		}
		for _, key in ipairs(seatLeftKeys) do
			addSortedKey(action.left, key)
		end
		local seatMagKeys = {
			{seatT+L*.030,cf(0,-.030,.008,2,0,0),"out"},
			{seatT+L*.070,CFrame.identity,"contact"},
			{seatT+L*.115,CFrame.identity,"impact"},
		}
		for _, key in ipairs(seatMagKeys) do
			addSortedKey(action.mag, key)
		end
	end

	action.weapon = compactReloadTrack(action.weapon)
	action.screen = subtleReloadScreen(action.length, spec.cycleOrder)
	action.continuousMotion = true
	action.hideRear = false
	action.cameraSafeScale = .92
	action.smoothing = {weapon=14,screen=11,camera=15,left=17,right=20,body=14,shoulder=14,pole=16,contact=38}
	action.humanity = action.humanity or {}
	action.humanity.microPos = 0
	action.humanity.microRotDeg = 0
	action.fumbleAt = nil -- mistakes are the five normal cycle variants, not rare extras
	action.disabled = false
	MAGFED[spec.key] = action
end


--------------------------------------------------------------------------------
-- V25 TRUE TACTICAL-RETENTION CYCLE
--
-- Five visually different live-magazine techniques.  Two are fresh-mag-first
-- exchanges based on the supplied references: the support hand brings a loaded
-- magazine to the gun before removing the partial one, then controls both
-- magazines in the same hand.  Three are old-mag-first carrier-retention
-- techniques.  The rifle itself only receives a small forward/up presentation;
-- the distinction comes from the hand and magazine choreography rather than a
-- dramatic whole-gun swing.
--------------------------------------------------------------------------------
local function addKeysV25(track: any, keys: any)
	for _, key in ipairs(keys) do
		addSortedKey(track, key)
	end
end

local v25RifleTacSpecs = {
	{
		key="reload_tactical", fresh=true, technique="rifle_fresh_first_l_index", cycleOrder=1,
		length=2.02, acquire=.15, magOut=.43, magIn=.72, durationScale=1.04,
		screen=cf(0,.052,-.132,0,0,0), work=cf(.006,.038,-.022,4.0,1.2,-3.0),
		bodyPouch=cf(-.37,-1.03,-.10,-44,20,12),
		spareStart=cf(-.19,-.74,.20,-15,8,-12), spareIndex=cf(-.105,-.31,.10,-8,4,-8),
		oldCarry=cf(-.165,-.43,.14,-18,10,-16),
		leftShoulder=Vector3.new(-.070,-.005,-.092), rightShoulder=Vector3.new(.014,-.006,-.030),
		mistake="fresh_mag_wobble",
	},
	{
		key="reload_tactical_b", fresh=true, technique="rifle_parallel_palm_stack", cycleOrder=2,
		length=2.10, acquire=.16, magOut=.45, magIn=.75, durationScale=1.08,
		screen=cf(0,.050,-.128,0,0,0), work=cf(.004,.036,-.020,3.0,-1.0,3.5),
		bodyPouch=cf(-.31,-1.07,-.11,-45,16,9),
		spareStart=cf(-.23,-.72,.17,-18,12,-17), spareIndex=cf(-.135,-.30,.075,-12,8,-14),
		oldCarry=cf(-.075,-.42,.12,-15,-7,11),
		leftShoulder=Vector3.new(-.055,-.018,-.090), rightShoulder=Vector3.new(.013,-.007,-.028),
		mistake="old_mag_grip_adjust",
	},
	{
		key="reload_tactical_c", technique="rifle_carrier_stow_front_lip", cycleOrder=3,
		length=2.16, magOut=.18, acquire=.55, contact=.72, magIn=.84, durationScale=1.11,
		screen=cf(0,.048,-.126,0,0,0), work=cf(.006,.033,-.019,4.5,.6,-3.2),
		bodyPouch=cf(-.39,-1.05,-.09,-46,22,13), miss=true, missX=.052,
		leftShoulder=Vector3.new(-.078,-.025,-.095), rightShoulder=Vector3.new(.013,-.009,-.030),
		mistake="front_lip_reindex",
	},
	{
		key="reload_tactical_d", fresh=true, technique="rifle_reverse_l_index_snag", cycleOrder=4,
		length=2.20, acquire=.21, magOut=.47, magIn=.77, durationScale=1.12,
		screen=cf(0,.050,-.130,0,0,0), work=cf(.006,.035,-.021,4.0,1.0,-2.0),
		bodyPouch=cf(-.42,-1.00,-.08,-47,24,15),
		spareStart=cf(.16,-.76,.18,-17,-9,14), spareIndex=cf(.105,-.33,.085,-10,-6,11),
		oldCarry=cf(.145,-.44,.12,-18,-7,15),
		leftShoulder=Vector3.new(-.083,-.040,-.098), rightShoulder=Vector3.new(.012,-.010,-.028),
		mistake="pouch_snag_then_exchange",
	},
	{
		key="reload_tactical_e", technique="rifle_retention_pull_check", cycleOrder=5,
		length=2.14, magOut=.18, acquire=.54, magIn=.81, tap=true, durationScale=1.09,
		screen=cf(0,.052,-.132,0,0,0), work=cf(.005,.039,-.021,4.5,.8,-3.0),
		bodyPouch=cf(-.35,-1.04,-.10,-44,19,12),
		leftShoulder=Vector3.new(-.066,-.012,-.090), rightShoulder=Vector3.new(.014,-.006,-.031),
		mistake="seat_pull_check",
	},
}

for _, spec in ipairs(v25RifleTacSpecs) do
	local action = spec.fresh and makeFreshFirstTac(spec) or makeOldFirstTac(spec)
	local L = action.length
	local acquireT = spec.acquire * L
	local outT = spec.magOut * L
	local seatT = spec.magIn * L

	if spec.mistake == "fresh_mag_wobble" then
		addKeysV25(action.left, {
			{outT-L*.16,{grip="magwell",offset=cf(-.135,-.335,.105,-10,4,-10)},"out"},
			{outT-L*.115,{grip="magwell",offset=cf(-.105,-.310,.090,-8,-1,5)},"linear"},
			{outT-L*.075,{grip="magwell",offset=cf(-.125,-.292,.082,-9,2,-5)},"linear"},
		})
		addKeysV25(action.spareMag, {
			{outT-L*.16,cf(-.135,-.335,.105,-10,4,-10),"out"},
			{outT-L*.115,cf(-.105,-.310,.090,-8,-1,5),"linear"},
			{outT-L*.075,cf(-.125,-.292,.082,-9,2,-5),"linear"},
		})
	elseif spec.mistake == "old_mag_grip_adjust" then
		-- Both magazines remain visible.  The partial magazine slips lower in the
		-- palm and is re-clamped before the loaded magazine is driven home.
		addKeysV25(action.left, {
			{outT+L*.055,{grip="magwell",offset=cf(-.060,-.455,.145,-17,-8,12)},"out"},
			{outT+L*.100,{grip="magwell",offset=cf(-.082,-.425,.125,-15,-5,9)},"linear"},
			{outT+L*.145,{grip="magwell",offset=cf(-.070,-.405,.115,-14,-6,10)},"out"},
		})
		addKeysV25(action.mag, {
			{outT+L*.055,cf(-.060,-.455,.145,-17,-8,12),"out"},
			{outT+L*.100,cf(-.082,-.425,.125,-15,-5,9),"linear"},
			{outT+L*.145,cf(-.070,-.405,.115,-14,-6,10),"out"},
		})
	elseif spec.mistake == "pouch_snag_then_exchange" then
		local pouch = spec.bodyPouch
		addKeysV25(action.leftBody, {
			{acquireT-L*.095,pouch*cf(-.028,-.015,.008,-2,3,-5),"out"},
			{acquireT-L*.050,pouch*cf(.018,.012,-.006,2,-2,4),"linear"},
			{acquireT,pouch,"out"},
		})
		addKeysV25(action.leftBodyWeight, {
			{acquireT-L*.12,1,"out"}, {acquireT+L*.03,1,"linear"},
		})
	elseif spec.mistake == "seat_pull_check" then
		addKeysV25(action.left, {
			{seatT+L*.025,{grip="magwell",offset=cf(0,-.030,.008,2,0,0)},"out"},
			{seatT+L*.065,{grip="magwell",offset=cf(0,.008,-.004,-1,0,0)},"contact"},
			{seatT+L*.105,{grip="magwell",offset=cf(0,.040,-.018,-3,0,0)},"impact"},
		})
		addKeysV25(action.mag, {
			{seatT+L*.025,cf(0,-.030,.008,2,0,0),"out"},
			{seatT+L*.065,CFrame.identity,"contact"},
			{seatT+L*.105,CFrame.identity,"impact"},
		})
	end

	-- Keep the weapon presentation subtle.  All five techniques differ through
	-- the support hand, old/new magazine relationship, elbow and pouch path.
	action.weapon = compactReloadTrack(action.weapon)
	action.screen = subtleReloadScreen(action.length, spec.cycleOrder)
	action.continuousMotion = true
	action.hideRear = false
	action.cameraSafeScale = .94
	action.smoothing = {weapon=18,screen=15,camera=18,left=20,right=24,body=18,shoulder=18,pole=20,contact=42}
	action.humanity = action.humanity or {}
	action.humanity.microPos = 0
	action.humanity.microRotDeg = 0
	action.fumbleAt = nil
	action.disabled = false

	-- Bone-locked support hand. The magazine bone is evaluated without the
	-- human-joint smoothing filter; previously the filtered hand lagged behind
	-- it by several frames and visibly slid through the palm. The attachment
	-- keeps the contact exact while approach/release remain authored motion.
		if spec.fresh then
			action.leftAttachBone = {{0,"none"},{acquireT-L*.035,"spareMag"},{seatT+L*.085,"none"}}
			action.leftAttachWeight = {{0,0},{acquireT-L*.055,0},{acquireT+L*.025,1,"out"},{seatT+L*.035,1,"linear"},{seatT+L*.095,0,"out"},{L,0,"linear"}}
		else
			action.leftAttachBone = {{0,"none"},{outT-L*.045,"mag"},{L,"mag"}}
			action.leftAttachWeight = {
				{0,0},{outT-L*.060,0},{outT+L*.025,1,"out"},{outT+L*.120,.72,"linear"},
				{acquireT-L*.095,0,"out"},{seatT-L*.165,0,"linear"},{seatT-L*.095,1,"out"},
				{seatT+L*.050,1,"linear"},{seatT+L*.115,0,"out"},{L,0,"linear"},
			}
		end
		action.leftAttachOffset = {{0,CFrame.identity}}
		-- Contact windows make the human joints converge rapidly only when metal
		-- and hand are physically touching; transit remains slower and organic.
		action.contactWindows = {
			{math.max(0, acquireT-L*.045), acquireT+L*.085},
			{math.max(0, outT-L*.050), outT+L*.130},
			{math.max(0, seatT-L*.125), seatT+L*.145},
		}
		action.smoothing.left = 23
		action.smoothing.body = 20
		action.smoothing.shoulder = 20
		action.smoothing.pole = 22
		action.smoothing.contact = 72
		MAGFED[spec.key] = action
end

--------------------------------------------------------------------------------
-- V25 PISTOL TACTICAL RETENTION
-- The supplied handgun reference holds the partial and loaded magazine in the
-- same support hand.  These five P320 techniques use the same two-magazine rig
-- rather than recycling the rifle's old-mag-to-pouch animation.
--------------------------------------------------------------------------------
local function makePistolRetentionV25(spec: any): any
	local L = spec.length
	local acquireT = spec.acquire * L
	local outT = spec.magOut * L
	local seatT = spec.magIn * L
	local freshFirst = spec.fresh == true
	local work = spec.work or cf(-.030,-.018,-.045,6,-5,7)
	local oldCarry = spec.oldCarry or cf(-.070,-.310,.105,-13,-5,10)
	local freshIndex = spec.freshIndex or cf(-.115,-.285,.105,-12,5,-11)
	local pouch = spec.bodyPouch or cf(-.34,-.98,-.10,-42,18,12)
	local a = {
		length=L, technique=spec.technique, cycleOrder=spec.cycleOrder, durationScale=spec.durationScale or 1,
		continuousMotion=true, hideRear=false, cameraSafeScale=.96,
		humanity={microPos=0,microRotDeg=0,leftArc=spec.leftArc or Vector3.new(-.012,.018,-.005)},
		smoothing={weapon=20,screen=17,camera=20,left=22,right=25,body=20,shoulder=20,pole=22,contact=44},
		screen=subtleReloadScreen(L,spec.cycleOrder),
		camera={{0,CFrame.identity},{L*.18,cf(-.002,-.004,.002,.10,-.06,.10),"out"},{L*.82,cf(-.002,-.004,.002,.10,-.06,.10),"linear"},{L,CFrame.identity,"settle"}},
		weapon={{0,CFrame.identity},{L*.055,cf(0,.008,-.012,-1,-.5,.7),"anticipate"},{L*.18,work,"out"},{L*.82,work,"linear"},{L*.93,cf(-.004,.008,-.014,-1.5,.4,-.8),"out"},{L,CFrame.identity,"settle"}},
		right={{0,"right"},{L*.20,{grip="right",offset=cf(0,-.006,.004,0,0,1.2)},"out"},{L*.86,{grip="right",offset=cf(0,-.006,.004,0,0,1.2)},"linear"},{L,"right","settle"}},
		leftPole=elbowPoleTrack(L,spec.leftPole or Vector3.new(-.08,-.04,-.08),.08,.90),
		rightPole=elbowPoleTrack(L,Vector3.new(.02,-.01,.02),.10,.90),
		leftShoulder=shoulderTracks(L,spec.leftShoulder or Vector3.new(-.055,-.012,-.080),Vector3.new(.008,-.004,-.018)).left,
		rightShoulder=shoulderTracks(L,spec.leftShoulder or Vector3.new(-.055,-.012,-.080),Vector3.new(.008,-.004,-.018)).right,
		leftBody={{0,pouch},{L,pouch,"linear"}},
	}
	if freshFirst then
		a.left={
			{0,"left"},{acquireT,{grip="pouch"},"out"},{acquireT+L*.08,{grip="magwell",offset=freshIndex},"out"},
			{outT,"magwell","contact"},{outT+L*.09,{grip="magwell",offset=oldCarry},"out"},
			{seatT-L*.08,{grip="magwell",offset=cf(-.025,-.125,.040,-5,1,-4)},"out"},{seatT,"magwell","impact"},
			{seatT+L*.08,{grip="magwell",offset=cf(0,.018,-.008,-2,0,0)},"out"},{L,"left","settle"},
		}
		a.mag={{0,CFrame.identity},{outT,CFrame.identity,"linear"},{outT+L*.09,oldCarry,"out"},{seatT+L*.10,oldCarry*cf(-.03,-.08,.02,-4,0,-4),"linear"},{seatT+L*.14,CFrame.identity,"snap"},{L,CFrame.identity,"linear"}}
		a.spareMag={{0,spec.spareStart or cf(-.20,-.68,.19,-16,8,-13)},{acquireT,spec.spareStart or cf(-.20,-.68,.19,-16,8,-13),"linear"},{acquireT+L*.08,freshIndex,"out"},{outT+L*.09,oldCarry*cf(.080,.035,-.025,4,-4,7),"out"},{seatT-L*.08,cf(-.025,-.125,.040,-5,1,-4),"out"},{seatT,CFrame.identity,"impact"},{L,CFrame.identity,"linear"}}
		a.magHide={{0,false},{seatT+L*.11,true},{seatT+L*.15,false}}
		a.spareMagHide={{0,true},{acquireT-L*.025,false},{seatT+L*.075,true}}
		a.leftBodyWeight={{0,0},{L*.04,0},{acquireT*.70,1,"out"},{acquireT+L*.04,1,"linear"},{acquireT+L*.12,0,"out"},{L,0,"linear"}}
	else
		a.left={
			{0,"left"},{outT-L*.08,{grip="magwell",offset=cf(-.010,.025,.005,3,0,-5)},"out"},{outT,"magwell","contact"},
			{outT+L*.09,{grip="magwell",offset=oldCarry},"out"},{acquireT-L*.06,{grip="pouch"},"out"},{acquireT,{grip="pouch"},"linear"},
			{seatT-L*.12,{grip="magwell",offset=freshIndex},"out"},{seatT,"magwell","impact"},{seatT+L*.07,{grip="magwell",offset=cf(0,.018,-.008,-2,0,0)},"out"},{L,"left","settle"},
		}
		a.mag={{0,CFrame.identity},{outT,CFrame.identity,"linear"},{outT+L*.09,oldCarry,"out"},{acquireT-L*.05,cf(-.30,-.72,.22,-28,12,18),"out"},{acquireT+L*.02,cf(-.30,-.72,.22,-28,12,18),"linear"},{seatT-L*.12,freshIndex,"snap"},{seatT,CFrame.identity,"impact"},{L,CFrame.identity,"linear"}}
		a.magHide={{0,false},{acquireT+L*.015,true},{seatT-L*.13,false}}
		a.leftBodyWeight={{0,0},{outT+L*.08,0},{outT+L*.16,1,"out"},{acquireT+L*.03,1,"linear"},{seatT-L*.14,0,"out"},{L,0,"linear"}}
	end
	return a
end

local v25PistolSpecs = {
	{key="reload_tactical",fresh=true,technique="pistol_fresh_first_l_index",cycleOrder=1,length=1.72,acquire=.16,magOut=.43,magIn=.72,mistake="wobble",freshIndex=cf(-.105,-.285,.105,-11,5,-10)},
	{key="reload_tactical_b",fresh=true,technique="pistol_parallel_palm_stack",cycleOrder=2,length=1.78,acquire=.17,magOut=.45,magIn=.74,mistake="grip_adjust",freshIndex=cf(-.075,-.300,.090,-13,-4,9),oldCarry=cf(-.030,-.325,.110,-14,-8,12)},
	{key="reload_tactical_c",technique="pistol_carrier_front_lip",cycleOrder=3,length=1.86,magOut=.18,acquire=.53,magIn=.82,mistake="front_lip",freshIndex=cf(-.085,-.285,.105,-12,5,-10)},
	{key="reload_tactical_d",fresh=true,technique="pistol_reverse_l_index_snag",cycleOrder=4,length=1.92,acquire=.22,magOut=.47,magIn=.76,mistake="pouch_snag",spareStart=cf(.14,-.69,.18,-16,-8,13),freshIndex=cf(.075,-.305,.095,-12,-5,10),oldCarry=cf(.095,-.33,.105,-14,-6,12)},
	{key="reload_tactical_e",technique="pistol_retention_pull_check",cycleOrder=5,length=1.84,magOut=.18,acquire=.52,magIn=.79,mistake="seat_check",freshIndex=cf(-.090,-.280,.100,-11,4,-9)},
}

for _, spec in ipairs(v25PistolSpecs) do
	local action = makePistolRetentionV25(spec)
	local L = action.length
	local acquireT, outT, seatT = spec.acquire*L, spec.magOut*L, spec.magIn*L
	if spec.mistake == "wobble" then
		addKeysV25(action.left, {{outT-L*.13,{grip="magwell",offset=cf(-.120,-.305,.115,-12,7,-12)},"out"},{outT-L*.085,{grip="magwell",offset=cf(-.095,-.285,.100,-10,-2,5)},"linear"},{outT-L*.045,{grip="magwell",offset=spec.freshIndex},"out"}})
		addKeysV25(action.spareMag, {{outT-L*.13,cf(-.120,-.305,.115,-12,7,-12),"out"},{outT-L*.085,cf(-.095,-.285,.100,-10,-2,5),"linear"},{outT-L*.045,spec.freshIndex,"out"}})
	elseif spec.mistake == "grip_adjust" then
		addKeysV25(action.mag, {{outT+L*.045,cf(-.020,-.355,.125,-15,-10,14),"out"},{outT+L*.095,spec.oldCarry,"out"}})
	elseif spec.mistake == "front_lip" then
		addKeysV25(action.left, {{seatT-L*.055,{grip="magwell",offset=cf(.035,-.050,.045,-4,0,8)},"contact"},{seatT-L*.020,{grip="magwell",offset=cf(-.020,-.115,.040,-6,0,-3)},"out"}})
		addKeysV25(action.mag, {{seatT-L*.055,cf(.035,-.050,.045,-4,0,8),"contact"},{seatT-L*.020,cf(-.020,-.115,.040,-6,0,-3),"out"}})
	elseif spec.mistake == "pouch_snag" then
		local pouch = action.leftBody[1][2]
		addKeysV25(action.leftBody, {{acquireT-L*.080,pouch*cf(-.025,-.012,.008,-2,2,-5),"out"},{acquireT-L*.035,pouch*cf(.015,.010,-.006,2,-1,4),"linear"},{acquireT,pouch,"out"}})
	elseif spec.mistake == "seat_check" then
		addKeysV25(action.left, {{seatT+L*.025,{grip="magwell",offset=cf(0,-.025,.006,2,0,0)},"out"},{seatT+L*.065,{grip="magwell",offset=cf(0,.008,-.004,-1,0,0)},"contact"},{seatT+L*.105,{grip="magwell",offset=cf(0,.033,-.014,-3,0,0)},"impact"}})
		addKeysV25(action.mag, {{seatT+L*.025,cf(0,-.025,.006,2,0,0),"out"},{seatT+L*.065,CFrame.identity,"contact"},{seatT+L*.105,CFrame.identity,"impact"}})
	end
	if spec.fresh then
			action.leftAttachBone = {{0,"none"},{acquireT-L*.030,"spareMag"},{seatT+L*.085,"none"}}
			action.leftAttachWeight = {{0,0},{acquireT-L*.050,0},{acquireT+L*.020,1,"out"},{seatT+L*.035,1,"linear"},{seatT+L*.095,0,"out"},{L,0,"linear"}}
		else
			action.leftAttachBone = {{0,"none"},{outT-L*.040,"mag"},{L,"mag"}}
			action.leftAttachWeight = {{0,0},{outT-L*.055,0},{outT+L*.020,1,"out"},{outT+L*.105,.70,"linear"},{acquireT-L*.080,0,"out"},{seatT-L*.145,0,"linear"},{seatT-L*.085,1,"out"},{seatT+L*.050,1,"linear"},{seatT+L*.110,0,"out"},{L,0,"linear"}}
		end
		action.leftAttachOffset = {{0,CFrame.identity}}
		action.contactWindows = {
			{math.max(0, acquireT-L*.040), acquireT+L*.080},
			{math.max(0, outT-L*.045), outT+L*.120},
			{math.max(0, seatT-L*.110), seatT+L*.135},
		}
		action.smoothing = action.smoothing or {}
		action.smoothing.left = 25
		action.smoothing.body = 22
		action.smoothing.shoulder = 22
		action.smoothing.pole = 24
		action.smoothing.contact = 76
		PISTOL[spec.key] = action
	end

	local function makeReferenceInspect(spec: any): any
	local L = spec.length
	return {
		length = L,
		technique = spec.technique,
		cycleOrder = spec.cycleOrder,
		humanity = { leftLeadMin=.006, leftLeadMax=.022, rightLagMin=.003, rightLagMax=.012, leftArc=spec.leftArc or Vector3.new(-.012,.025,-.006), microPos=0, microRotDeg=0 },
		camera = {
			{0,CFrame.identity}, {L*.18,cf(-.002,-.003,0,0.08,-0.04,0.08),"out"},
			{L*.78,cf(-.002,-.003,0,0.08,-0.04,0.08),"linear"}, {L,CFrame.identity,"settle"},
		},
		screen = {
			{0,CFrame.identity}, {L*.14,spec.screen,"out"}, {L*.80,spec.screen,"linear"}, {L,CFrame.identity,"settle"},
		},
		weapon = {
			{0,CFrame.identity}, {L*.05,cf(0,.006,-.006,-.7,-.4,.4),"anticipate"},
			{L*.20,spec.poseA,"heavy"}, {L*.46,spec.poseB or spec.poseA,"out"},
			{L*.68,spec.poseC or spec.poseA,"out"}, {L*.82,cf(.004,.008,-.018,.6,1.4,-.8),"out"},
			{L,CFrame.identity,"settle"},
		},
		left = {
			{0,"left"}, {L*.14,spec.handA,"out"}, {L*.38,spec.handB or spec.handA,"out"},
			{L*.60,spec.handC or spec.handB or spec.handA,"out"}, {L*.82,"left","out"}, {L,"left","settle"},
		},
		right = { {0,"right"}, {L*.82,{grip="right",offset=cf(-.002,.004,-.004,-.3,.2,-.2)},"out"}, {L,"right","settle"} },
		leftPole = elbowPoleTrack(L, spec.leftPole or Vector3.new(-.10,.03,.10), .08,.88),
		rightPole = elbowPoleTrack(L, Vector3.new(.015,0,.02), .10,.90),
		leftShoulder = shoulderTracks(L, spec.leftShoulder or Vector3.new(-.04,.02,-.05), Vector3.new(.005,0,-.012)).left,
		rightShoulder = shoulderTracks(L, spec.leftShoulder or Vector3.new(-.04,.02,-.05), Vector3.new(.005,0,-.012)).right,
	}
end

local referenceInspectSpecs = {
	{ key="inspect", technique="ref_inspect_receiver", cycleOrder=1, length=2.35, screen=cf(-.025,.035,-.020,0,0,0),
		poseA=cf(-.035,.075,-.025,8,24,-18), poseB=cf(.010,.090,-.020,12,-14,12), poseC=cf(-.020,.060,-.020,6,18,-13),
		handA={grip="magwell",offset=cf(-.02,.10,.02,-8,0,-8)}, handB={grip="charge",offset=cf(-.02,.02,.03,0,0,-12)}, handC={grip="left",offset=cf(0,.04,-.02,3,0,-4)} },
	{ key="inspect_b", technique="ref_inspect_ejection_port", cycleOrder=2, length=2.20, screen=cf(-.020,.030,-.018,0,0,0),
		poseA=cf(.035,.065,-.022,7,-24,17), poseB=cf(.055,.080,-.018,9,-31,22), poseC=cf(.015,.050,-.020,5,-12,9),
		handA={grip="magwell",offset=cf(.03,.08,.03,-6,0,8)}, handB={grip="charge",offset=cf(.02,.01,.02,0,0,10)}, handC="left", leftArc=Vector3.new(.025,.030,-.004) },
	{ key="inspect_c", technique="ref_inspect_controls", cycleOrder=3, length=2.10, screen=cf(-.018,.025,-.016,0,0,0),
		poseA=cf(-.060,.040,-.020,4,32,-22), poseB=cf(-.075,.055,-.018,6,38,-26), poseC=cf(-.025,.045,-.020,4,16,-11),
		handA={grip="release",offset=cf(-.02,.03,.02,0,0,-8)}, handB={grip="right",offset=cf(-.08,.20,-.10,-12,8,-20)}, handC="left" },
	{ key="inspect_d", technique="ref_inspect_magwell", cycleOrder=4, length=2.30, screen=cf(-.022,.030,-.018,0,0,0),
		poseA=cf(-.030,.090,-.025,14,20,-15), poseB=cf(-.050,.110,-.020,18,30,-22), poseC=cf(-.010,.055,-.020,7,10,-7),
		handA={grip="magwell",offset=cf(0,.10,.03,-10,0,0)}, handB={grip="magwell",offset=cf(-.04,-.04,.05,4,0,-8)}, handC="left", leftArc=Vector3.new(-.020,.050,-.006) },
	{ key="inspect_e", technique="ref_inspect_handguard", cycleOrder=5, length=2.18, screen=cf(-.015,.025,-.016,0,0,0),
		poseA=cf(.020,.055,-.022,6,-17,13), poseB=cf(.050,.065,-.020,8,-28,20), poseC=cf(.010,.045,-.018,4,-8,6),
		handA={grip="left",offset=cf(0,.12,.05,-10,0,0)}, handB={grip="left",offset=cf(.05,.04,-.18,2,-4,9)}, handC="left", leftArc=Vector3.new(.025,.035,-.004) },
	{ key="inspect_f", technique="ref_inspect_optic_mount", cycleOrder=6, length=2.25, screen=cf(-.020,.030,-.018,0,0,0),
		poseA=cf(-.020,.085,-.024,11,16,-12), poseB=cf(-.005,.105,-.020,15,4,-3), poseC=cf(-.015,.055,-.018,6,12,-9),
		handA={grip="charge",offset=cf(-.03,.14,-.08,-12,0,-10)}, handB={grip="charge",offset=cf(.02,.08,-.12,-7,0,5)}, handC="left", leftArc=Vector3.new(-.010,.055,-.008) },
}
for _, spec in referenceInspectSpecs do
	MAGFED[spec.key] = makeReferenceInspect(spec)
end

Animations.MAGFED = MAGFED
Animations.TUBE = TUBE
Animations.PISTOL = PISTOL

function Animations.forWeapon(def: any)
	if def.id == "P320" then
		return PISTOL
	end
	return def.feed == "tube" and TUBE or MAGFED
end


--------------------------------------------------------------------------------
-- Evaluation
--------------------------------------------------------------------------------

local function easeFn(name: string?)
	return EASE[name or "quad"] or EASE.quad
end

-- Generic keyframe sample. `resolve` turns a raw track value into the type the
-- caller wants; `blend` interpolates two resolved values.
local function sample(track, t: number, resolve, blend)
	if not track or #track == 0 then
		return nil
	end
	if t <= track[1][1] or #track == 1 then
		return resolve(track[1][2])
	end
	local last = track[#track]
	if t >= last[1] then
		return resolve(last[2])
	end
	for i = 2, #track do
		local a, b = track[i - 1], track[i]
		if t <= b[1] then
			local span = b[1] - a[1]
			local f = span > 0 and (t - a[1]) / span or 1
			return blend(resolve(a[2]), resolve(b[2]), easeFn(b[3])(f))
		end
	end
	return resolve(last[2])
end

local function sampleStep(track, t: number, default: any): any
	if not track then
		return default
	end
	local v = default
	for _, kf in track do
		if t >= kf[1] then
			v = kf[2]
		else
			break
		end
	end
	return v
end

local function blendCF(a: CFrame, b: CFrame, f: number): CFrame
	return a:Lerp(b, f)
end

-- Hand targets should not travel on ruler-straight lines. A restrained lift arc
-- gives the elbow room to lead the hand and keeps pouch-to-magwell reaches from
-- reading as two CFrames being linearly dragged through space. Endpoints remain
-- exact, so mechanical contacts still line up with the weapon bones.
local function blendHandCF(a: CFrame, b: CFrame, f: number, arcBias: Vector3?): CFrame
	-- sample() has already applied the authored easing. The old implementation
	-- ran that value through smootherstep a second time, forcing the hand to
	-- stop at every keyframe. Raw segment time keeps velocity alive through the
	-- path while the Bezier controls provide the natural elbow-led arc.
	local t = math.clamp(f, 0, 1)
	local p0, p3 = a.Position, b.Position
	local delta = p3 - p0
	local distance = delta.Magnitude
	local bias = arcBias or Vector3.zero
	local lift = math.min(0.11, distance * 0.13)
	local side = math.min(0.042, distance * 0.050)
	local p1 = p0 + delta * 0.23 + Vector3.new(side, lift, -side * 0.38) + bias
	local p2 = p0 + delta * 0.77 + Vector3.new(-side * 0.30, lift * 0.38, side * 0.18) + bias * 0.32
	local u = 1 - t
	local pos = p0 * (u * u * u)
		+ p1 * (3 * u * u * t)
		+ p2 * (3 * u * t * t)
		+ p3 * (t * t * t)

	-- The wrist follows the elbow/hand translation instead of rotating in lockstep.
	-- A four-percent delay is enough to read as a joint chain without making the
	-- magazine arrive at the magwell crooked.
	local rotT = math.clamp((t - 0.04) / 0.96, 0, 1)
	rotT = rotT * rotT * (3 - 2 * rotT)
	local rot = a:Lerp(b, rotT)
	return CFrame.new(pos) * (rot - rot.Position)
end

local function blendNum(a: number, b: number, f: number): number
	return a + (b - a) * f
end
local function blendVec(a: Vector3, b: Vector3, f: number): Vector3
	return a:Lerp(b, f)
end
local function passthrough(v: any): any
	return v
end

-- Continuous cubic sampling for human motion. The old evaluator treated every
-- keyframe as a tiny start/stop instruction, so even a dense timeline still
-- looked like the rifle snapped from pose to pose. These Hermite samplers use
-- neighbouring keys to preserve velocity across segment boundaries. Magazine,
-- bolt, and charging-handle tracks still use the ordinary sampler so metal-on-
-- metal contacts remain crisp.
local function hermiteVec(p0: Vector3, p1: Vector3, p2: Vector3, p3: Vector3,
	t0: number, t1: number, t2: number, t3: number, u: number, tension: number?): Vector3
	local span = math.max(t2 - t1, 1e-4)
	local v1 = (p2 - p0) / math.max(t2 - t0, 1e-4)
	local v2 = (p3 - p1) / math.max(t3 - t1, 1e-4)
	local scale = tension or 0.72
	local m1 = v1 * span * scale
	local m2 = v2 * span * scale
	local u2 = u * u
	local u3 = u2 * u
	local h00 = 2 * u3 - 3 * u2 + 1
	local h10 = u3 - 2 * u2 + u
	local h01 = -2 * u3 + 3 * u2
	local h11 = u3 - u2
	return p1 * h00 + m1 * h10 + p2 * h01 + m2 * h11
end

local function smoothSegment(track, t: number)
	if not track or #track == 0 then
		return nil
	end
	if t <= track[1][1] or #track == 1 then
		return 1, 1, 0
	end
	local last = #track
	if t >= track[last][1] then
		return last, last, 0
	end
	for i = 2, last do
		if t <= track[i][1] then
			local a = i - 1
			local span = track[i][1] - track[a][1]
			local u = span > 1e-5 and (t - track[a][1]) / span or 1
			return a, i, math.clamp(u, 0, 1)
		end
	end
	return last, last, 0
end

local function sampleSmoothCF(track, t: number, resolve, tension: number?): CFrame?
	local i1, i2, u = smoothSegment(track, t)
	if not i1 then
		return nil
	end
	if i1 == i2 then
		return resolve(track[i1][2])
	end
	local i0 = math.max(1, i1 - 1)
	local i3 = math.min(#track, i2 + 1)
	local c0 = resolve(track[i0][2])
	local c1 = resolve(track[i1][2])
	local c2 = resolve(track[i2][2])
	local c3 = resolve(track[i3][2])
	local p = hermiteVec(
		c0.Position, c1.Position, c2.Position, c3.Position,
		track[i0][1], track[i1][1], track[i2][1], track[i3][1], u, tension
	)
	-- Smooth angular velocity too. CFrame:Lerp is smooth inside a segment but its
	-- angular velocity changes abruptly at every key. Reloads contain many keys,
	-- so those tiny angular corners were the remaining "snap" the eye caught.
	local function unwrap(reference: number, value: number): number
		while value - reference > math.pi do value -= math.pi * 2 end
		while value - reference < -math.pi do value += math.pi * 2 end
		return value
	end
	local x0, y0, z0 = c0:ToOrientation()
	local x1, y1, z1 = c1:ToOrientation()
	local x2, y2, z2 = c2:ToOrientation()
	local x3, y3, z3 = c3:ToOrientation()
	x0, y0, z0 = unwrap(x1, x0), unwrap(y1, y0), unwrap(z1, z0)
	x2, y2, z2 = unwrap(x1, x2), unwrap(y1, y2), unwrap(z1, z2)
	x3, y3, z3 = unwrap(x2, x3), unwrap(y2, y3), unwrap(z2, z3)
	local function hs(a0, a1, a2, a3)
		local v = hermiteVec(
			Vector3.new(a0, 0, 0), Vector3.new(a1, 0, 0),
			Vector3.new(a2, 0, 0), Vector3.new(a3, 0, 0),
			track[i0][1], track[i1][1], track[i2][1], track[i3][1], u, tension
		)
		return v.X
	end
	return CFrame.new(p) * CFrame.Angles(hs(x0, x1, x2, x3), hs(y0, y1, y2, y3), hs(z0, z1, z2, z3))
end

local function sampleSmoothVec(track, t: number, tension: number?): Vector3?
	local i1, i2, u = smoothSegment(track, t)
	if not i1 then
		return nil
	end
	if i1 == i2 then
		return track[i1][2]
	end
	local i0 = math.max(1, i1 - 1)
	local i3 = math.min(#track, i2 + 1)
	return hermiteVec(
		track[i0][2], track[i1][2], track[i2][2], track[i3][2],
		track[i0][1], track[i1][1], track[i2][1], track[i3][1], u, tension
	)
end

local function sampleSmoothNum(track, t: number, tension: number?): number?
	local i1, i2, u = smoothSegment(track, t)
	if not i1 then
		return nil
	end
	if i1 == i2 then
		return track[i1][2]
	end
	local i0 = math.max(1, i1 - 1)
	local i3 = math.min(#track, i2 + 1)
	local p = hermiteVec(
		Vector3.new(track[i0][2], 0, 0), Vector3.new(track[i1][2], 0, 0),
		Vector3.new(track[i2][2], 0, 0), Vector3.new(track[i3][2], 0, 0),
		track[i0][1], track[i1][1], track[i2][1], track[i3][1], u, tension
	)
	return p.X
end


local function actionHandPoses(name: string?, t: number, anim: any): (string, string)
	if not name or not anim then
		return "foregrip", "firing"
	end
	local length = anim.length or 1
	if name:find("^reload_") or name:find("^inspect") then
		if t < 0.11 or t > length - 0.18 then
			return "foregrip", "firing"
		end
		local technique = anim.technique
		if technique == "l_index" or technique == "l_index_retention" or technique == "fast_l_index"
			or (type(technique) == "string" and (technique:find("fresh_first") or technique:find("reverse_l"))) then
			return "dual_mag", "firing"
		elseif technique == "pouch_snag" or technique == "empty_pouch_snag" or technique == "emergency_magwell_miss" then
			return "hook", "firing"
		elseif type(technique) == "string" and technique:find("charging_handle") and t > length * 0.62 then
			return "pinch", "firing"
		end
		return "magazine", "firing"
	elseif name == "check_mag" then
		return (t > 0.12 and t < length - 0.12) and "magazine" or "foregrip", "firing"
	elseif name == "check_chamber" then
		return (t > 0.10 and t < length - 0.08) and "pinch" or "foregrip", "firing"
	elseif name:find("^inspect") then
		return "open", "firing"
	elseif name == "load_shell" or name:find("^shells_load") then
		return (t > 0.08 and t < length - 0.08) and "shell" or "foregrip", "firing"
	elseif name == "fire_selector" then
		return "controls", "firing"
	end
	return "foregrip", "firing"
end

--------------------------------------------------------------------------------
-- Player
--------------------------------------------------------------------------------
local Player = {}
Player.__index = Player

function Animations.newPlayer(def: any)
	local self = setmetatable({
		def = def,
		set = Animations.forWeapon(def),

		anim = nil,
		name = nil,
		time = 0,
		scale = 1,
		looping = false,

		weight = 0,
		fadingOut = false,
		FADE_IN = 0.105,
		FADE_OUT = 0.165,

		-- Auto-loaders cycle too fast to animate with keyframes; a short
		-- triangle ramp reads better and never fights a reload timeline.
		kick = 0,
		KICK_LEN = 0.085,
		boltLocked = false,
		lastVariant = nil, -- so a variant never repeats back to back
		variantCursor = {}, -- reload technique cycle; fumbles remain uncommon
		reloadCounter = {}, -- every sixth reload is allowed to use a fumble
		motionSerial = 0,
		human = {
			leftLead = 0,
			rightLag = 0,
			cameraLag = 0,
			weaponLead = 0,
			leftArc = Vector3.zero,
			rightArc = Vector3.zero,
			phase = 0,
		},

		pose = {
			camera = CFrame.identity,
			screen = CFrame.identity,
			weapon = CFrame.identity,
			bones = {},
			left = CFrame.identity,
			right = CFrame.identity,
			-- Optional shoulder/body-local support-hand target. Reload pouch work
			-- must not orbit with the rifle; this decouples the hand from the gun.
			leftBody = CFrame.identity,
			leftBodyWeight = 0,
			leftAttachBone = "none",
			leftAttachWeight = 0,
			leftAttachOffset = CFrame.identity,
			-- During a hold the support hand is the master and the magazine is the
			-- child. This avoids a filtered hand chasing an unfiltered mechanical
			-- bone and visibly sliding through the palm.
			heldMagBone = "none",
			heldMagWeight = 0,
			leftHandPose = "foregrip",
			rightHandPose = "firing",
			leftPole = Vector3.zero,
			rightPole = Vector3.zero,
			leftShoulder = Vector3.zero,
			rightShoulder = Vector3.zero,
			magHidden = false,
			spareMagHidden = true,
			hideRear = false,
			cameraSafeScale = 1,
		},
		filtered = {
			camera = CFrame.identity,
			screen = CFrame.identity,
			weapon = CFrame.identity,
			left = def.grips.left,
			right = def.grips.right,
			leftBody = CFrame.identity,
			leftBodyWeight = 0,
			leftPole = Vector3.zero,
			rightPole = Vector3.zero,
			leftShoulder = Vector3.zero,
			rightShoulder = Vector3.zero,
		},
	}, Player)
	return self
end

function Player:setWeapon(def: any)
	self.def = def
	self.set = Animations.forWeapon(def)
	self.filtered.camera = CFrame.identity
	self.filtered.screen = CFrame.identity
	self.filtered.weapon = CFrame.identity
	self.filtered.left = def.grips.left
	self.filtered.right = def.grips.right
	self.filtered.leftBody = CFrame.identity
	self.filtered.leftBodyWeight = 0
	self.filtered.leftPole = Vector3.zero
	self.filtered.rightPole = Vector3.zero
	self.filtered.leftShoulder = Vector3.zero
	self.filtered.rightShoulder = Vector3.zero
	self:stop()
end

-- duration: stretch the animation to exactly this many seconds. Pass the real
-- mechanical time from GameConfig.HANDLING and the motion always lines up with
-- what the gun is actually doing.
-- VARIATIONS. `play("reload_tactical")` will pick at random between
-- reload_tactical, reload_tactical_b, reload_tactical_c and so on, whichever
-- exist. Only mechanically equivalent variants should use these suffixes.
-- Repetition is the thing that breaks the illusion fastest -- the tenth
-- identical reload reads as a machine no matter how good the first one was --
-- and it costs nothing but a suffix on a table key.
--
-- Never picks the variant it just played, so you cannot get the same one twice
-- in a row even at two variants.
local VARIANT_SUFFIX = {
	"", "_b", "_c", "_d", "_e", "_f", "_g", "_h",
	"_i", "_j", "_k", "_l", "_m", "_n",
}

function Player:resolve(name: string): string
	local options = nil
	for suffixIndex, sfx in VARIANT_SUFFIX do
		local key = name .. sfx
		local candidate = self.set[key]
		if candidate and not candidate.disabled then
			options = options or {}
			table.insert(options, {
				key = key,
				weight = candidate.chanceWeight or 1,
				fumble = candidate.fumbleAt ~= nil,
				cycleOrder = candidate.cycleOrder or suffixIndex,
			})
		end
	end
	if not options or #options < 2 then
		return name
	end

	-- Reloads use a technique cycle rather than pure random selection. The player
	-- should be able to press R three times and immediately see three different
	-- methods; pure randomness made a large library look like one animation.
	if name:find("^reload_") then
		local core = {}
		local fumbles = {}
		for _, option in options do
			if option.fumble then
				table.insert(fumbles, option)
			else
				table.insert(core, option)
			end
		end

		-- A fumble is an interruption, not a normal reload style. Every sixth
		-- reload may use one, which makes the detail testable without turning a
		-- trained operator into slapstick. The first five remain clean.
		local counterKey = tostring(self.def.id) .. ":" .. name
		local count = (self.reloadCounter[counterKey] or 0) + 1
		self.reloadCounter[counterKey] = count
		if #fumbles > 0 and count % 6 == 0 then
			local option = fumbles[math.random(1, #fumbles)]
			self.lastVariant = option.key
			return option.key
		end

		if #core > 0 then
			table.sort(core, function(a, b)
				return a.cycleOrder < b.cycleOrder
			end)
			local cursorKey = tostring(self.def.id) .. ":" .. name
			local previous = self.variantCursor[cursorKey]
			local cursor = (previous or 0) + 1
			if cursor > #core then
				cursor = 1
			end
			self.variantCursor[cursorKey] = cursor
			local pick = core[cursor].key
			self.lastVariant = pick
			return pick
		end
	end

	-- Non-reload actions keep weighted random selection.
	local totalWeight = 0
	for _, option in options do
		if option.key ~= self.lastVariant then
			totalWeight += option.weight
		end
	end
	if totalWeight <= 0 then
		return options[1].key
	end
	local roll = math.random() * totalWeight
	local pick = options[1].key
	for _, option in options do
		if option.key ~= self.lastVariant then
			roll -= option.weight
			if roll <= 0 then
				pick = option.key
				break
			end
		end
	end

	self.lastVariant = pick
	return pick
end


-- Returns the real duration and selected animation table. Fumble variants may
-- take 8-20% longer; callers use this duration for the mechanical state too, so
-- a visibly missed magwell cannot finish reloading before the second attempt.
local function startResolved(self: any, name: string, duration: number?)
	local anim = self.set[name]
	if not anim then
		return nil, name, nil
	end
	self.anim = anim
	self.name = name
	self.time = 0
	self.looping = anim.loop == true
	local baseDuration = (duration and duration > 0) and duration or anim.length
	local actualDuration = baseDuration * (anim.durationScale or 1)
	self.scale = anim.length / math.max(actualDuration, 1e-4)
	self.fadingOut = false

	self.motionSerial += 1
	local seed = (self.motionSerial * 48271 + math.floor(os.clock() * 100000)) % 2147483646 + 1
	local r = Random.new(seed)
	local h = anim.humanity or {}
	self.human = {
		leftLead = r:NextNumber(h.leftLeadMin or -0.012, h.leftLeadMax or 0.018),
		rightLag = r:NextNumber(h.rightLagMin or -0.004, h.rightLagMax or 0.012),
		cameraLag = r:NextNumber(h.cameraLagMin or 0.004, h.cameraLagMax or 0.018),
		weaponLead = r:NextNumber(h.weaponLeadMin or -0.006, h.weaponLeadMax or 0.006),
		leftArc = (h.leftArc or Vector3.zero) + Vector3.new(
			r:NextNumber(-0.012, 0.012), r:NextNumber(-0.008, 0.014), r:NextNumber(-0.010, 0.010)
		),
		rightArc = (h.rightArc or Vector3.zero) + Vector3.new(
			r:NextNumber(-0.006, 0.006), r:NextNumber(-0.004, 0.008), r:NextNumber(-0.006, 0.006)
		),
		phase = r:NextNumber(0, math.pi * 2),
	}
	return actualDuration, name, anim
end

function Player:play(name: string, duration: number?)
	return startResolved(self, self:resolve(name), duration)
end

-- Starts an exact already-resolved variant. Remote clients use this so the
-- shooter and every observer see the same reload technique rather than each
-- machine independently cycling to a different suffix.
function Player:playResolved(name: string, duration: number?)
	return startResolved(self, name, duration)
end

function Player:stop()
	if self.anim then
		self.fadingOut = true
	end
end

function Player:isPlaying(name: string?): boolean
	if not self.anim or self.fadingOut then
		return false
	end
	return name == nil or self.name == name
end

function Player:kickBolt()
	self.kick = self.KICK_LEN
end

function Player:update(dt: number)
	local def = self.def
	local pose = self.pose
	local grips = def.grips

	-- Resolvers need the bone table for `follow`, so bones are evaluated first.
	local bones = pose.bones
	for k in bones do
		bones[k] = nil
	end

	local anim = self.anim
	local t = 0

	if anim then
		self.time += dt * self.scale
		if self.time >= anim.length then
			if self.looping then
				self.time %= anim.length
			else
				self.time = anim.length
				self.fadingOut = true
			end
		end
		t = self.time

		if self.fadingOut then
			self.weight -= dt / self.FADE_OUT
			if self.weight <= 0 then
				self.weight = 0
				self.anim = nil
				self.name = nil
				self.fadingOut = false
				anim = nil
			end
		else
			self.weight = math.min(1, self.weight + dt / self.FADE_IN)
		end
	else
		self.weight = 0
	end

	local w = self.weight

	----------------------------------------------------------------------------
	-- Bones
	----------------------------------------------------------------------------
	local travel = def.boltTravel or 0.2

	local boltFrac = nil
	if anim and anim.bolt then
		boltFrac = sample(anim.bolt, t, passthrough, blendNum)
	end
	if boltFrac == nil then
		-- No timeline is driving the bolt, so the mechanical state does.
		if self.boltLocked then
			boltFrac = 1
		elseif self.kick > 0 then
			self.kick = math.max(0, self.kick - dt)
			local phase = 1 - self.kick / self.KICK_LEN -- 0 -> 1 across the cycle
			boltFrac = phase < 0.30 and (phase / 0.30) or (1 - (phase - 0.30) / 0.70)
		else
			boltFrac = 0
		end
	else
		boltFrac = boltFrac * w -- fade the timeline's bolt in with the rest
		if self.boltLocked then
			boltFrac = math.max(boltFrac, 1 - w)
		end
	end
	if boltFrac > 0.001 then
		bones.bolt = CFrame.new(0, 0, boltFrac * travel)
	end

	local pumpFrac = anim and anim.pump and sample(anim.pump, t, passthrough, blendNum) or 0
	pumpFrac *= w
	if pumpFrac > 0.001 then
		bones.pump = CFrame.new(0, 0, pumpFrac * travel)
	end

	if anim and anim.charge then
		local f = sample(anim.charge, t, passthrough, blendNum) * w
		if f > 0.001 then
			bones.charge = CFrame.new(0, 0, f * travel)
		end
	end

	if anim and anim.mag then
		local cf = sample(anim.mag, t, passthrough, blendCF)
		bones.mag = CFrame.identity:Lerp(cf, w)
	end

	if anim and anim.spareMag then
		local cf = sample(anim.spareMag, t, passthrough, blendCF)
		bones.spareMag = CFrame.identity:Lerp(cf, w)
	end

	----------------------------------------------------------------------------
	-- Hands. Resolved after bones so `follow` can read them.
	----------------------------------------------------------------------------
	local function resolveHand(v: any): CFrame
		if typeof(v) == "CFrame" then
			return v
		end
		if type(v) == "string" then
			return grips[v] or CFrame.identity
		end
		if type(v) == "table" then
			local base = grips[v.grip] or CFrame.identity
			if v.follow then
				base = (bones[v.follow] or CFrame.identity) * base
			end
			if v.offset then
				base = base * v.offset
			end
			return base
		end
		return CFrame.identity
	end

	local restLeft = grips.left
	local restRight = grips.right

	if anim then
		local human = self.human
		local length = math.max(anim.length or 1, 1e-4)
		local function shifted(seconds: number): number
			return math.clamp(t + seconds, 0, length)
		end
		local leftT = shifted(human.leftLead)
		local rightT = shifted(-human.rightLag)
		local cameraT = shifted(-human.cameraLag)
		local weaponT = shifted(human.weaponLead)

		local continuous = anim.continuousMotion == true
		local l = continuous
			and sampleSmoothCF(anim.left, leftT, resolveHand, 0.68)
			or sample(anim.left, leftT, resolveHand, function(a, b, f)
				return blendHandCF(a, b, f, human.leftArc)
			end)
		local r = continuous
			and sampleSmoothCF(anim.right, rightT, resolveHand, 0.72)
			or sample(anim.right, rightT, resolveHand, function(a, b, f)
				return blendHandCF(a, b, f, human.rightArc)
			end)
		l = l or restLeft
		r = r or restRight
		pose.left = restLeft:Lerp(l, w)
		pose.right = restRight:Lerp(r, w)
		local bodyTarget = continuous
			and sampleSmoothCF(anim.leftBody, leftT, passthrough, 0.66)
			or sample(anim.leftBody, leftT, passthrough, blendCF)
		bodyTarget = bodyTarget or CFrame.identity
		local bodyWeight = continuous
			and sampleSmoothNum(anim.leftBodyWeight, leftT, 0.58)
			or sample(anim.leftBodyWeight, leftT, passthrough, blendNum)
		bodyWeight = bodyWeight or 0
		pose.leftBody = bodyTarget
		pose.leftBodyWeight = math.clamp(bodyWeight * w, 0, 1)
		-- Contact metadata follows raw mechanical time, not the support-hand lead.
		-- The magazine bone and the attachment now enter/leave contact on the same
		-- frame, eliminating the remaining one-to-three-frame palm slip.
		pose.leftAttachBone = anim.leftAttachBone and sampleStep(anim.leftAttachBone, t, "none") or "none"
		local attachWeight = sample(anim.leftAttachWeight, t, passthrough, blendNum) or 0
		pose.leftAttachWeight = math.clamp(attachWeight * w, 0, 1)
		pose.leftAttachOffset = sample(anim.leftAttachOffset, t, passthrough, blendCF) or CFrame.identity
		pose.heldMagBone = pose.leftAttachBone
		pose.heldMagWeight = pose.leftAttachWeight
		pose.leftHandPose, pose.rightHandPose = actionHandPoses(self.name, leftT, anim)
		local leftPole = continuous
			and sampleSmoothVec(anim.leftPole, leftT, 0.62)
			or sample(anim.leftPole, leftT, passthrough, blendVec)
		local rightPole = continuous
			and sampleSmoothVec(anim.rightPole, rightT, 0.66)
			or sample(anim.rightPole, rightT, passthrough, blendVec)
		leftPole = leftPole or Vector3.zero
		rightPole = rightPole or Vector3.zero
		pose.leftPole = Vector3.zero:Lerp(leftPole, w)
		pose.rightPole = Vector3.zero:Lerp(rightPole, w)
		local leftShoulder = continuous
			and sampleSmoothVec(anim.leftShoulder, leftT, 0.60)
			or sample(anim.leftShoulder, leftT, passthrough, blendVec)
		local rightShoulder = continuous
			and sampleSmoothVec(anim.rightShoulder, rightT, 0.64)
			or sample(anim.rightShoulder, rightT, passthrough, blendVec)
		leftShoulder = leftShoulder or Vector3.zero
		rightShoulder = rightShoulder or Vector3.zero
		pose.leftShoulder = Vector3.zero:Lerp(leftShoulder, w)
		pose.rightShoulder = Vector3.zero:Lerp(rightShoulder, w)
		local cameraCF = continuous
			and sampleSmoothCF(anim.camera, cameraT, passthrough, 0.58)
			or sample(anim.camera, cameraT, passthrough, blendCF)
		cameraCF = cameraCF or CFrame.identity
		pose.camera = CFrame.identity:Lerp(cameraCF, w)
		local screenCF = CFrame.identity
		if typeof(anim.screen) == "CFrame" then
			screenCF = anim.screen
		elseif type(anim.screen) == "table" then
			screenCF = continuous
				and sampleSmoothCF(anim.screen, weaponT, passthrough, 0.58)
				or sample(anim.screen, weaponT, passthrough, blendCF)
			screenCF = screenCF or CFrame.identity
		end
		pose.screen = CFrame.identity:Lerp(screenCF, w)
		local cf = continuous
			and sampleSmoothCF(anim.weapon, weaponT, passthrough, 0.64)
			or sample(anim.weapon, weaponT, passthrough, blendCF)
		cf = cf or CFrame.identity

		-- Low-frequency muscular correction, strongest in transit and zero at the
		-- endpoints. This is deliberately sub-pixel at normal FOV; it breaks the
		-- mathematically perfect spline without becoming camera shake.
		local p = math.clamp(weaponT / length, 0, 1)
		local envelope = math.sin(math.pi * p) ^ 2
		local hc = anim.humanity or {}
		local posAmp = hc.microPos or 0
		local rotAmp = math.rad(hc.microRotDeg or 0)
		local phase = human.phase
		local micro = CFrame.new(
			math.sin(p * 9.1 + phase) * posAmp * envelope,
			math.sin(p * 6.7 + phase * 0.71) * posAmp * 0.65 * envelope,
			math.sin(p * 7.9 + phase * 1.17) * posAmp * 0.55 * envelope
		) * CFrame.Angles(
			math.sin(p * 7.3 + phase) * rotAmp * envelope,
			math.sin(p * 5.9 + phase * 0.53) * rotAmp * 0.75 * envelope,
			math.sin(p * 8.5 + phase * 1.31) * rotAmp * 0.90 * envelope
		)
		pose.weapon = CFrame.identity:Lerp(cf * micro, w)
		pose.hideRear = anim.hideRear == true and w > 0.03
		pose.cameraSafeScale = anim.cameraSafeScale or 1
		pose.magHidden = w > 0.5 and sampleStep(anim.magHide, t, false) or false
		pose.spareMagHidden = not (w > 0.5 and anim.spareMag)
		if anim.spareMag then
			pose.spareMagHidden = sampleStep(anim.spareMagHide, t, false) or w <= 0.5
		end
	else
		pose.left = restLeft
		pose.right = restRight
		pose.leftBody = CFrame.identity
		pose.leftBodyWeight = 0
		pose.leftAttachBone = "none"
		pose.leftAttachWeight = 0
		pose.leftAttachOffset = CFrame.identity
		pose.heldMagBone = "none"
		pose.heldMagWeight = 0
		pose.leftHandPose = "foregrip"
		pose.rightHandPose = "firing"
		pose.leftPole = Vector3.zero
		pose.rightPole = Vector3.zero
		pose.leftShoulder = Vector3.zero
		pose.rightShoulder = Vector3.zero
		pose.camera = CFrame.identity
		pose.screen = CFrame.identity
		pose.weapon = CFrame.identity
		pose.hideRear = false
		pose.cameraSafeScale = 1
		pose.magHidden = false
		pose.spareMagHidden = true
	end


	-- Final human-motion filter. The authored target remains mechanically exact,
	-- but joints cannot change velocity instantaneously. A short critically
	-- damped-looking chase removes the last spline corners without turning metal
	-- contacts into mush. Contact windows temporarily raise the rate so the hand
	-- and magazine still arrive together.
	do
		local f = self.filtered
		local s = (anim and anim.smoothing) or {}
		local inContact = false
		if anim and anim.contactWindows then
			for _, window in anim.contactWindows do
				if t >= window[1] and t <= window[2] then inContact = true break end
			end
		end
		local contactRate = s.contact or 46
		local function alpha(rate: number): number
			if inContact then rate = math.max(rate, contactRate) end
			return 1 - math.exp(-math.max(rate, 1) * math.min(dt, 1/30))
		end
		f.weapon = f.weapon:Lerp(pose.weapon, alpha(s.weapon or 24))
		f.screen = f.screen:Lerp(pose.screen, alpha(s.screen or 20))
		f.camera = f.camera:Lerp(pose.camera, alpha(s.camera or 22))
		f.left = f.left:Lerp(pose.left, alpha(s.left or 30))
		f.right = f.right:Lerp(pose.right, alpha(s.right or 30))
		f.leftBody = f.leftBody:Lerp(pose.leftBody, alpha(s.body or 25))
		f.leftBodyWeight += (pose.leftBodyWeight - f.leftBodyWeight) * alpha(s.body or 25)
		f.leftPole = f.leftPole:Lerp(pose.leftPole, alpha(s.pole or 28))
		f.rightPole = f.rightPole:Lerp(pose.rightPole, alpha(s.pole or 28))
		f.leftShoulder = f.leftShoulder:Lerp(pose.leftShoulder, alpha(s.shoulder or 24))
		f.rightShoulder = f.rightShoulder:Lerp(pose.rightShoulder, alpha(s.shoulder or 24))
		pose.weapon, pose.screen, pose.camera = f.weapon, f.screen, f.camera
		pose.left, pose.right = f.left, f.right
		pose.leftBody, pose.leftBodyWeight = f.leftBody, f.leftBodyWeight
		pose.leftPole, pose.rightPole = f.leftPole, f.rightPole
		pose.leftShoulder, pose.rightShoulder = f.leftShoulder, f.rightShoulder
	end

	-- The support hand rides the pump even when no timeline says so, so that
	-- firing a shotgun never leaves the hand hanging in the air.
	if bones.pump and not (anim and anim.left) then
		pose.left = bones.pump * pose.left
	end

	return pose
end

Animations.EASE = EASE

return Animations
