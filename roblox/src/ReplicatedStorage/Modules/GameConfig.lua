--!nonstrict
-- GameConfig -- every tunable number in the game lives here.
-- Nothing else in the codebase is allowed to hardcode a gameplay constant.
--
-- UNITS: Roblox studs. This project treats 1 stud = 0.35 m (an R15 character is
-- ~5 studs tall and reads as a ~6 ft human). Real-world values in comments are
-- converted with STUDS_PER_METER so you can sanity-check them against reality.

local C = {}

--------------------------------------------------------------------------------
-- WHICH MAP
--
--   "ARENA"     daylight test box: cover at every useful height, a zeroing
--               range, free-for-all spawns, no entity, no objectives. This is
--               the one to be in while working on PVP and on weapon feel.
--   "FACILITY"  the real game -- procedural blacksite, dark, SUBJECT_09.
--
-- One line. MapBuilder is untouched either way.
--------------------------------------------------------------------------------
C.MODE = "ARENA"

-- Seconds before you come back. Characters are loaded by hand (see GameServer)
-- so this is not Players.RespawnTime -- that only applies with CharacterAutoLoads.
C.RESPAWN_TIME = 6.0

C.STUDS_PER_METER = 2.857 -- 1 stud = 0.35 m

local function m(meters: number): number
	return meters * C.STUDS_PER_METER
end
C.m = m

--------------------------------------------------------------------------------
-- MOVEMENT
--------------------------------------------------------------------------------
C.MOVE = {
	-- studs/s. Real m/s in comments.
	SPRINT = m(5.4), -- 5.4 m/s  hard run, weapon at low ready, cannot fire
	WALK = m(2.6), -- 2.6 m/s  default patrol pace
	SLOW = m(1.2), -- 1.2 m/s  Ctrl -- deliberately quiet
	CROUCH = m(1.4), -- 1.4 m/s
	ADS = m(1.7), -- 1.7 m/s  while aiming down sights

	-- Stamina drains while sprinting, refills otherwise.
	STAMINA_MAX = 9.0, -- seconds of continuous sprint
	STAMINA_REGEN = 0.55, -- units per second (slower than it drains)
	STAMINA_SPRINT_MIN = 1.5, -- must have this much left to *start* a sprint

	EYE_HEIGHT_STAND = 2.5, -- camera height above HumanoidRootPart centre
	EYE_HEIGHT_CROUCH = 1.1,

	LEAN_ANGLE = math.rad(21), -- Q/E roll
	LEAN_OFFSET = 1.15, -- studs the camera slides sideways when leaning
	-- How fast you get there is C.STANCE, not a blend rate. Both crouch and
	-- lean are springs now.
}

--------------------------------------------------------------------------------
-- CAMERA / VIEWMODEL FEEL
--------------------------------------------------------------------------------
C.CAM = {
	SENS = 0.0022, -- radians per pixel of mouse movement
	SENS_ADS_SCALE = 0.55, -- sights slow your turn rate, like real optics
	PITCH_LIMIT = math.rad(85),

	-- V17 reference pass: the OPERATOR clip uses a tighter, larger weapon presentation.
	FOV_HIP = 74,
	FOV_ADS = 48, -- per-weapon optics multiply this (see Weapons.luau)
	FOV_SPRINT = 80,
	FOV_BLEND = 8.0,

	FREELOOK_YAW = math.rad(85), -- Alt: look around without turning the body
	FREELOOK_PITCH = math.rad(60),

	BOB_WALK = 0.055, -- viewmodel bob amplitude
	BOB_SPRINT = 0.140,
	-- A footfall fires every PI radians of this phase, so the step rate is
	-- BOB_SPEED * speedFrac / PI per second. At 9.5 that was 3.0 steps/s
	-- walking and 6.3 SPRINTING -- roughly double a real cadence, so the
	-- footfalls blurred into a buzz and stopped reading as individual steps at
	-- all. That is a large part of "it feels like I'm sliding": the impulses
	-- were firing, they were just firing too fast to be legible as feet.
	-- 6.0 gives 1.9 walking and 4.0 sprinting, which is human.
	BOB_SPEED = 6.0,

	-- Unaimed breathing/muscle sway. Real shooters cannot hold a gun still.
	BREATH_AMPLITUDE_HIP = 0.020,
	-- Raised: this is the ONLY thing still moving when you are stood still on
	-- the sights holding your breath, so if it is inaudible then ADS is dead.
	BREATH_AMPLITUDE_ADS = 0.0115,
	BREATH_AMPLITUDE_HELD = 0.0012, -- while holding breath
	BREATH_SPEED = 1.15,
	BREATH_MAX = 6.0, -- seconds you can hold your breath
	BREATH_RECOVER = 0.42, -- fraction of a second of recovery per second held
}

--------------------------------------------------------------------------------
-- STANCE SPRINGS
--
-- Crouching, leaning and aiming used to be exponential lerps. A lerp
-- asymptotes: it starts fast, ends slow, and never overshoots, which is
-- exactly why it reads as weightless -- nothing with mass moves like that.
-- These are springs. The overshoot is small but it is the entire difference
-- between "the value changed" and "a body moved".
--------------------------------------------------------------------------------
C.STANCE = {
	-- Softened hard from the first pass. Going down on one knee is not a 0.2 s
	-- event; at K 52 / D 9.6 this takes most of a second and visibly settles.
	CROUCH_K = 52,
	CROUCH_D = 9.6, -- zeta 0.67
	CROUCH_IMPULSE = 0.95, -- weapon drops as your knees give

	LEAN_K = 62,
	LEAN_D = 9.4, -- zeta 0.60: leans settle visibly past and back
	LEAN_IMPULSE = 0.65,

	-- ANALOG STANCE. OPERATOR's headline movement feature is that crouch and
	-- lean are not two-state -- you hold the key and the MOUSE sets the exact
	-- height and the exact angle, so you can put your eye precisely at the top
	-- of a crate instead of over-leaning into the room.
	--
	-- Tap the key (under TAP_TIME) and you get the old toggle. Hold it and the
	-- mouse takes over for as long as you hold. Aim does not move while you
	-- are steering your body -- that is the trade.
	-- LEAN IS HOLD-TO-LEAN and does NOT steal the mouse. Analog lean trim was a
	-- mistake in practice: leaning is something you do to peek and then fight
	-- from, and taking the mouse away for the duration means you cannot aim at
	-- the thing you just leaned out to look at. Crouch keeps it, because you
	-- pick a height before the contact rather than during it.
	ANALOG_LEAN = false,
	ANALOG_CROUCH = true,
	TAP_TIME = 0.20, -- under this, a crouch press is a toggle
	LEAN_PER_PIXEL = 0.0060, -- lean units (0..1) per pixel of mouse X
	CROUCH_PER_PIXEL = 0.0045, -- stance units (0..1) per pixel of mouse Y
	ANALOG_MIN = 0.14, -- below this the pose snaps back to neutral
}

C.ADS = {
	RATE = 6.0, -- k = (RATE / adsTime)^2
	DAMP = 0.72, -- well under critical, so the sights settle in rather than arrive
	RAISE_IMPULSE = 0.95, -- kick into the weapon spring when presenting
	DROP_IMPULSE = 0.55, -- ...and when coming off the sights
	MAX_ALPHA = 1.09, -- Lerp extrapolates: the gun settles INTO the shoulder
}

--------------------------------------------------------------------------------
-- RECOIL
--
-- Recoil used to be two numbers added to the camera's pitch. That is a screen
-- effect, not a gun going off: the whole world tilted and the weapon, being a
-- rigid child of the camera, did not move relative to it AT ALL. You could not
-- see your own rifle recoil.
--
-- Now it happens in three places, which is what actually happens:
--   1. A permanent climb written into the BORE. You pull it back down.
--   2. A transient rotation of the WEAPON about its shoulder pocket -- the
--      part you watch. This is why the pivot matters: rotating about the butt
--      pad throws the muzzle through an arc and leaves the receiver under your
--      eye roughly where it was, which is the shape your eye knows.
--   3. Velocity into the head and torso springs, so the impulse travels back
--      through the body and your view rocks because your CHEST was hit.
--------------------------------------------------------------------------------
C.RECOIL = {
	-- V17: the reference clips keep the camera composed and put the sharp event
	-- into the rifle. The receiver snaps rearward/up, then settles with a small
	-- residual climb instead of smearing the entire screen.
	RISE_IMPULSE = 54,
	SIDE_IMPULSE = 25,
	BACK_IMPULSE = 16,

	MAX_RISE = math.rad(9.5),
	MAX_SIDE = math.rad(3.3),
	MAX_BACK = 0.22,

	K = 255,
	DAMP = 0.66,
	RECOVER_REF = 11.0,

	HEAD_IMPULSE = 1.8,
	TORSO_IMPULSE = 1.3,

	PERMANENT = 0.21,

	-- Separate high-frequency ignition impulse. Main recoil carries the mass;
	-- this layer supplies the sharp first contact and is gone in under 0.1 s.
	SNAP_K = 520,
	SNAP_D = 27,
	SNAP_BACK = 13.5,
	SNAP_PITCH = 30,
	SNAP_YAW = 15,
	SNAP_ROLL = 9,
	SNAP_POS_X = 0.08,
	SNAP_POS_Y = 0.035,
}

--------------------------------------------------------------------------------
-- AIMING
--
-- How long it takes to get the sights up is a property of the weapon, not a
-- global constant -- an 8 lb carbine and a 1.8 lb pistol do not present at the
-- same speed. Each weapon declares `adsTime` in seconds and the spring rate is
-- derived from it, so tuning the feel is tuning one honest number.
--------------------------------------------------------------------------------
C.VIEWMODEL = {
	-- The weapon sits closer to the camera than the old flat-parts viewmodel
	-- did, because the IK arms have to be able to reach the handguard from a
	-- shoulder anchored behind the camera. Moving HIP means re-checking that
	-- the support hand still lands inside ARMS reach at both hip and aim.
	-- The slight cant is deliberate: a weapon held square to the camera reads
	-- as a prop bolted to the screen. Three degrees of yaw and four of roll is
	-- enough to make it look carried.
	-- These are now SHOULDER-relative, not camera-relative: the weapon hangs
	-- off the chest via C.ARMS.SHOULDER_*, so Y is measured down from the
	-- shoulder plane rather than down from the eye. That is why every Y here
	-- moved up by about 0.6 stud compared with the old numbers.
	-- Shoulder-relative. The conversion from the old camera-relative numbers is
	-- exact: the eye sits at (0, 0.75, -0.34) in the shoulder frame, so
	--     shoulderPose = eyePose + (0, 0.75, -0.34)
	-- I got this wrong on the first pass -- I subtracted the full 0.60 from Y
	-- and left Z alone, which put the grip 0.73 studs below the eye and only
	-- 0.28 in front of it. That is 69 degrees down: the weapon was hanging
	-- almost directly under the camera, which is exactly the "looking at the
	-- gun from above and to the right" in the screenshots.
	--
	-- X is pulled in from the old 0.62 to the shoulder line (ARMS.SHOULDER_R is
	-- 0.42) because the weapon hangs off a shoulder now, not off the eye, and a
	-- rifle at the ready sits under the right pec rather than out beside the ribs.
	-- V16: closer, shoulder-pocket presentation. The previous -0.96 Z placed the
	-- receiver nearly a full stud forward of the grip and made the rifle read as
	-- a distant prop. Rear furniture is independently faded, so the weapon can
	-- sit where a shouldered carbine belongs without drawing inside the eye.
	-- V17 OPERATOR reference framing. The receiver is close and fills the lower-right
	-- quadrant while the muzzle stays near the visual centre. Rear furniture is
	-- camera-faded, so bringing the grip closer does not put a stock in the eye.
	HIP = CFrame.new(0.34, 0.07, -0.52) * CFrame.Angles(math.rad(-0.7), math.rad(-5.5), math.rad(1.4)),
	LOW_READY = CFrame.new(0.34, -0.08, -0.53) * CFrame.Angles(math.rad(-25), math.rad(8), math.rad(0.5)),
	SPRINT = CFrame.new(0.30, -0.15, -0.49) * CFrame.Angles(math.rad(-39), math.rad(23), math.rad(8)),
	HIGH_PORT = CFrame.new(0.30, 0.18, -0.47) * CFrame.Angles(math.rad(49), math.rad(8), 0),

	BLEND = 9.0, -- legacy fallback; the live path below uses a mass-rated spring.

	-- Carry-state transitions (hip / low ready / sprint / wall compression).
	-- These used to be CFrame:Lerp calls, so every weapon moved with the same
	-- weightless exponential curve. K is scaled by weapon mass in FPSClient:
	--     k_weapon = K * (REFERENCE_MASS / weaponMass)^MASS_EXP
	-- and d = 2*sqrt(k_weapon)*DAMP.
	CARRY_POS_K = 72,
	CARRY_ROT_K = 58,
	CARRY_DAMP = 0.68,
	CARRY_REFERENCE_MASS = 3.1, -- kg, MK18 reference
	CARRY_MASS_EXP = 0.38,
	-- Presenting the sights is not here: it is a per-weapon spring, see C.ADS.
	-- Nor is the ADS POSE -- that is solved every frame against the camera's
	-- optical axis (Weapons.adsSolve), not authored as a CFrame, so the sights
	-- line up correctly no matter what the head, chest and lean are doing.
	ADS_EYE_RELIEF = 0.30, -- studs the optical centre sits ahead of the eye

	-- SIGHT TRIM. Added to the weapon's sightOffset when solving the aim pose.
	-- The iron sight height was derived from the converted mesh's bounding
	-- boxes, which gives the sight PART's centre and not the aperture -- so it
	-- can be a couple of hundredths out, and a couple of hundredths at the rear
	-- aperture is a visible misalignment at the front post.
	--
	-- I cannot see the screen, so this is the knob: run with -Serve, aim, and
	-- nudge Y until the front post sits in the rear notch. Positive Y raises
	-- the point the sight is aligned to, which drops the weapon in view.
	ADS_TRIM = Vector3.new(0, 0.054, 0),

	-- MESH LOOK. Enum.Material.Metal on a MeshPart leans on environment
	-- reflections, and this game runs EnvironmentSpecularScale at 0 indoors --
	-- so Metal plus a dark colour renders very nearly black. SmoothPlastic is
	-- lit by diffuse only and is the safe default for a viewmodel.
	-- Published-safe by default. EditableMesh can be blocked in live servers even
	-- when it works in Studio, causing the weapon to silently change models.
	-- Native parts are serialized in the place and therefore render identically
	-- in Studio, private servers and the published experience.
	RUNTIME_MESH = true,
	MESH_MATERIAL = Enum.Material.SmoothPlastic,
	-- 86,89,94 overshot into near-white in daylight. SmoothPlastic takes the
	-- full diffuse term with no reflection to darken it, so it needs a much
	-- lower value than Metal did: this is anodised-black territory.
	-- No texture or surface map is available on the runtime mesh, so tonal
	-- separation has to come from the material colours themselves.
	-- V16 dark anodised receiver palette. The old 142-value SmoothPlastic plus a
	-- 1.2-brightness blue fill light is exactly why the rifle became white in the
	-- daylight arena. These values preserve readable separation without bleaching.
	-- Neutral anodised-black palette. The old blue fill was the remaining cause
	-- of the white receiver in bright maps. This is intentionally dark, but the
	-- bolt, magazine and controls remain separated by value.
	MESH_COLOR = Color3.fromRGB(18, 21, 25),
	MESH_DETAIL_COLOR = Color3.fromRGB(9, 11, 14),
	MESH_BOLT_COLOR = Color3.fromRGB(67, 68, 65),
	MESH_MAG_COLOR = Color3.fromRGB(22, 25, 27),
	FILL_BRIGHTNESS = 0.06,
	FILL_RANGE = 2.0,
	FILL_COLOR = Color3.fromRGB(148, 146, 141),
	NEAR_CLIP_SAFE = 0.18,
	NEAR_CLIP_MAX_PUSH = 0.18,
	-- Renders faces regardless of winding. Cheap insurance: if the source mesh
	-- has any inverted normals you see the geometry rather than a black hole.
	MESH_DOUBLE_SIDED = true,

	-- WEAPON COLLISION. This used to be a boolean: raycast hits something,
	-- snap to HIGH_PORT. A hard discontinuity you walk into is the single most
	-- jarring thing in the old build. It is continuous now -- the closer the
	-- muzzle gets, the further the weapon is pushed up and back, so you can
	-- feel the wall through the gun before it has swung anywhere.
	COLLISION_CHECK = 3.6, -- studs ahead of the shoulder the bore is checked
	COLLISION_PULL = 0.55, -- fraction of the blocked distance pulled back in Z
	COLLISION_ADS_BLOCK = 0.62, -- past this much push you cannot get on sights
}

--------------------------------------------------------------------------------
-- ARMS -- two-bone IK rig for the first-person hands.
--
-- These are NOT anatomical proportions and are not meant to be. The shoulders
-- sit behind and below the camera where they are never drawn; the bone lengths
-- exist so that the visible part of the arm -- the forearm entering frame from
-- below -- reaches the weapon's grip anchors at both hip and aim without going
-- fully straight. The firing arm is much shorter than the support arm because
-- the firing hand is always close to the body and the support hand is a long
-- way down the handguard.
--
-- If you move VIEWMODEL.HIP, check both distances again. Overshooting reach is
-- not fatal -- the arm just straightens and the hand still lands correctly --
-- but a permanently straight arm looks dead.
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
-- THE BODY -- arms, head, torso, hips.
--
-- This replaces the old single free-aim deadzone, and it is the structural fix
-- for four rounds of "stiff / weightless".
--
-- The old system clamped the weapon angle to an ellipse and dumped the
-- overflow straight into the camera yaw. Two things follow from that, and
-- both of them are exactly the complaint:
--
--   1. A clamp is the stiffest object you can build. d(weapon)/d(mouse) is 1
--      inside the ellipse and 0 on the boundary, and during any sustained turn
--      the weapon sits PINNED on that boundary -- perfectly rigid relative to
--      the camera, for the whole turn.
--   2. The camera was an instantaneous algebraic function of the mouse. The
--      weapon was then a rigid child of an instantaneous parent, so every
--      "weight" term was a small correction on a transform that had already
--      snapped. No amount of spring on the child fixes a rigid parent.
--
-- What is here instead is a chain of four bodies, each dragged by the one in
-- front of it through a DEAD-BAND SPRING -- a joint with slack, then stiffness:
--
--     mouse -> ARMS (bore) -> HEAD (camera) -> TORSO (holds the weapon) -> HIPS
--
-- The mouse drives the ARMS in world space, 1:1, with zero latency. Rounds
-- fire along the arm axis, so none of the lag below costs a millisecond of
-- aim -- which is the whole reason free aim and a lagged camera can coexist.
--
-- The deadzone that stops the horizon swimming is now HEAD_DZ, and it is
-- small (2.6 deg). The deadzone that makes the gun sweep across the screen
-- when you turn is TORSO_DZ, and it is large (9.5 deg) -- because the torso
-- is what the weapon hangs off. Splitting those two jobs onto two joints is
-- the thing the old one-deadzone version could not do.
--
-- Damping ratios below are all under 1 on purpose. zeta = D / (2*sqrt(K)).
--------------------------------------------------------------------------------
C.BODY = {
	-- HEAD -- the camera. Chases the bore.
	-- Small slack, high stiffness: a 2 degree correction moves only the gun,
	-- a fast flick drags the view with visible follow-through and settle.
	HEAD_DZ_X = math.rad(2.6),
	HEAD_DZ_Y = math.rad(2.0),
	HEAD_KNEE = math.rad(1.7), -- width of the C1 soft knee. See deadband().
	HEAD_K = 260, -- f ~2.6 Hz
	HEAD_D = 26, -- zeta 0.81 -- settles in one small overshoot
	HEAD_MAX = math.rad(24), -- hard backstop; the head is shoved, not the bore

	-- TORSO -- carries the weapon. Its lag IS the gun swinging on screen.
	TORSO_DZ_X = math.rad(9.5),
	TORSO_DZ_Y = math.rad(6.0),
	TORSO_KNEE = math.rad(4.0),
	TORSO_K = 95, -- f ~1.6 Hz -- a chest is heavy
	TORSO_D = 13, -- zeta 0.67 -- it visibly passes the mark and comes back
	TORSO_MAX = math.rad(40),
	TORSO_PITCH_SCALE = 0.35, -- the chest pitches far less than the neck does

	-- HIPS -- movement direction. Very loose: you can look over your shoulder
	-- without your feet following, and turning while running makes you arc.
	-- COSMETIC ONLY as of v9. The hips used to supply the movement basis, which
	-- meant W sent you up to 34 degrees away from where you were looking and
	-- the harder you turned the worse it got -- "running to where you're looking
	-- feels weird", exactly. Movement now comes off the HEAD; these angles only
	-- rotate the visible character, which nobody in first person can see.
	HIPS_DZ = math.rad(24),
	HIPS_KNEE = math.rad(9),
	HIPS_K = 46,
	HIPS_D = 10.0, -- zeta 0.74
	HIPS_MAX = math.rad(70),
	HIPS_WALK_ALIGN = 60, -- stiffness ADDED while moving: a walking person squares up

	-- Aiming tightens the chain, it does not weld it. The first pass collapsed
	-- the deadzones to 10% and stiffened K by 2.1x, which took ADS all the way
	-- back to the rigid v6 behaviour -- correctly reported as "too stiff while
	-- aiming". A braced rifle still moves; it just moves less and settles
	-- faster. 30% slack and 1.35x stiffness keeps the body in the loop.
	ADS_DZ_SCALE = 0.30,
	ADS_STIFFEN = 1.35, -- multiplies K on head and torso (D scales as sqrt)

	-- Anatomy, in studs. 1 stud = 0.35 m.
	CHEST_TO_NECK = 1.05, -- 0.37 m, waist-mounted chest origin to atlas
	CHEST_TO_SHOULDER = 0.60, -- shoulder plane, where the arms hang from
	NECK_TO_EYE = Vector3.new(0, 0.30, -0.34), -- eyes are IN FRONT of the neck
	WAIST_PIVOT = Vector3.new(0, -0.95, 0), -- lean rotates the chest about this

	-- Turning your head fast rolls it slightly into the turn and back. Tiny,
	-- and one of the largest single contributors to "there is a person here".
	VESTIBULAR_ROLL = 0.055, -- radians of roll per rad/s of head yaw rate
	VESTIBULAR_MAX = math.rad(2.4),
	VESTIBULAR_K = 120,
	VESTIBULAR_D = 14,
}

--------------------------------------------------------------------------------
-- INERTIA -- the weight of the weapon.
--
-- This is the most recognisable thing about OPERATOR's feel and it is not
-- "sway". Sway is a sine wave. Inertia is a mass on a spring: whip the camera
-- around and the gun is left behind, swings past the new direction, and
-- settles. The damping below is deliberately LESS than critical
-- (critical = 2*sqrt(K), so ~24 for ROT_K) because the overshoot is the part
-- that reads as weight. Raise the damping toward 24 and the gun starts feeling
-- like it is made of foam.
--------------------------------------------------------------------------------
-- The old version drove these springs by camera angular RATE and by linear
-- VELOCITY. That is the wrong derivative and it is why doubling the lag
-- coefficients did nothing: a rate-proportional target produces a constant
-- offset during a constant-rate turn (the gun just looks mounted crooked) and
-- produces NOTHING at the start and the stop of the turn -- which are pure
-- acceleration events, and which are the only two moments that carry weight
-- information. Bigger coefficient, bigger crooked offset, same dead start.
--
-- Now the spring targets are ZERO and the drive is a pseudo-force. The
-- viewmodel is a mass sitting in an accelerating reference frame, so it feels
-- -m*a exactly the way your hands do, and the sustained-turn trailing that
-- used to come from the rate term now comes from the torso joint in C.BODY,
-- where it physically belongs.
C.INERTIA = {
	ROT_K = 78, -- f ~1.4 Hz
	ROT_D = 10.5, -- zeta 0.59 -- overshoots, which is the part that reads
	POS_K = 62, -- f ~1.25 Hz -- the gun is heavier than it was
	POS_D = 9.0, -- zeta 0.57

	-- Pseudo-force gains. Radians of swing per rad/s^2 of head angular
	-- acceleration, studs per stud/s^2 of body linear acceleration.
	ROT_ACCEL = 0.0125,
	ROT_ROLL_COUPLE = 0.55, -- yaw acceleration also banks the muzzle
	POS_ACCEL = 0.0075,

	MAX_ROT = 0.30, -- ~17 degrees of swing at full whip
	MAX_POS = 0.26, -- studs

	ADS_SCALE = 0.45, -- braced into the shoulder, but a braced rifle still moves

	-- Impulses fired into the position spring.
	STEP_IMPULSE = 0.34, -- every footfall
	LAND_IMPULSE = 2.60, -- hitting the ground

	-- Idle drift. The gun is never still, not even when you are.
	IDLE_AMP = 0.0165,
	IDLE_AMP_ADS = 0.0038,
	IDLE_SPEED = 0.62,
	IDLE_FATIGUE = 2.4, -- tired arms wander more. See C.ARM_STAMINA.

	-- Fixed-step integration. Anything stiffer than about K=200 goes unstable
	-- on a 60 ms frame under explicit Euler, and the old min(dt, 1/30) clamp
	-- meant every spring in the game ran in SLOW MOTION below 30 fps.
	SUBSTEP = 1 / 240,
	MAX_SUBSTEPS = 8,
}

--------------------------------------------------------------------------------
-- CAMERA MOTION
--
-- The weapon was doing all the moving and the camera was welded to a tripod,
-- which is why walking felt weightless no matter what the gun did. These are
-- small on purpose -- large camera bob is nauseating -- but their absence is
-- very obvious.
--------------------------------------------------------------------------------
C.CAMSHAKE = {
	-- Footfalls are now IMPULSES into a spring rather than a raw sine written
	-- onto position. A sine is a shape; an impulse into a damped spring is an
	-- event, and the difference is audible in the same way a drum machine and
	-- a drummer differ. STEP_* is the kick, BOB_* is the small residual sway
	-- that rides on top of it.
	-- These were far too big and that is most of "running feels weird".
	--
	-- The impulse is STEP_DIP * sqrt(STEP_K) * f, and peak displacement is
	-- v0/omega = v0/sqrt(STEP_K). So the peak head drop is just STEP_DIP * f --
	-- and f was speedFrac (up to 1.6) TIMES SPRINT_STEP_MULT (1.9) = 3.04.
	-- 0.17 * 3.04 = a HALF STUD of camera drop per sprint step, four times a
	-- second. That is not weight, that is a seizure.
	--
	-- Now: peak drop is 0.065 * 1.8 = 0.12 studs (4 cm) sprinting and 0.065
	-- (2 cm) walking, with the total multiplier hard-capped.
	STEP_DIP = 0.065, -- studs the head drops when a foot lands
	STEP_ROLL = math.rad(0.7), -- and rolls onto that foot
	STEP_PUSH = 0.035, -- and gets checked along its own travel direction
	STEP_K = 150,
	STEP_D = 15, -- zeta 0.61
	SPRINT_STEP_MULT = 1.35, -- a run lands harder than a walk, not four times harder
	STEP_FORCE_MAX = 1.8, -- hard cap on the combined speed * sprint multiplier

	BOB_POS = 0.030, -- residual sway, much smaller now the kick carries it
	BOB_LATERAL = 0.028,
	BOB_ROLL = math.rad(0.40),

	STRAFE_ROLL = math.rad(2.2), -- banking into a sidestep
	STRAFE_K = 55,
	STRAFE_D = 11,

	LAND_DIP = 0.50, -- studs the camera drops on landing
	LAND_K = 78, -- softer than before: knees absorb over ~0.4 s, not 0.2
	LAND_D = 10.5,

	ADS_SCALE = 0.55, -- braced, so the head steadies -- but it does not stop
}

--------------------------------------------------------------------------------
-- WEAPON GAIT
--
-- Camera footfalls are impulses, but the gun still needs the cyclic motion of
-- arms and shoulders between impacts. This is six-axis and asymmetric: lateral
-- hand travel, vertical compression, fore/aft carry, plus pitch/yaw/roll.
-- Values are intentionally small because the step and inertia springs carry the
-- actual impacts.
--------------------------------------------------------------------------------
C.GAIT = {
	POS_X = 0.036,
	POS_Y = 0.026,
	POS_Z = 0.020,
	PITCH = math.rad(0.52),
	YAW = math.rad(0.82),
	ROLL = math.rad(1.18),

	SPRINT_MULT = 1.55,
	CROUCH_MULT = 0.58,
	SLOW_MULT = 0.72,
	ADS_SCALE = 0.20,
	MAX_SCALE = 1.75,
}

--------------------------------------------------------------------------------
-- CUSTOM SOLDIER LOCOMOTION
--
-- Drives the actual R15 Motor6D skeleton underneath the custom mesh. Roblox's
-- default Animate script is disabled locally, so the same authored procedural
-- motion is used in first person and for every replicated third-person soldier.
--------------------------------------------------------------------------------
C.SOLDIER_ANIM = {
	WALK_CADENCE = 7.1,          -- radians/sec at normal walking speed
	SPRINT_CADENCE = 9.7,
	CROUCH_CADENCE = 5.5,
	SMOOTH = 14.0,
	PHASE_SMOOTH = 10.0,

	WALK_HIP = math.rad(26),
	SPRINT_HIP = math.rad(43),
	CROUCH_HIP = math.rad(15),
	STRAFE_HIP_ROLL = math.rad(8),
	KNEE_FLEX = math.rad(43),
	SPRINT_KNEE = math.rad(60),
	CROUCH_KNEE = math.rad(46),
	ANKLE_COUNTER = math.rad(18),

	PELVIS_BOB = 0.055,
	PELVIS_SWAY = 0.045,
	PELVIS_YAW = math.rad(2.2),
	TORSO_COUNTER = math.rad(2.8),
	SPRINT_LEAN = math.rad(10.5),
	WALK_LEAN = math.rad(2.0),
	CROUCH_DROP = 0.34,
	CROUCH_FORWARD = 0.10,
	ADS_GAIT_SCALE = 0.34,
	ADS_CHEST_FORWARD = 0.045,
	ADS_HEAD_LOWER = 0.020,

	JUMP_TUCK_HIP = math.rad(24),
	JUMP_TUCK_KNEE = math.rad(42),
	FALL_EXTEND_HIP = math.rad(-8),
	FALL_KNEE = math.rad(14),
	LAND_COMPRESS = math.rad(32),
	LAND_TIME = 0.24,
}

--------------------------------------------------------------------------------
-- THE HUMAN SKELETON
--
-- Tuning for the joints R15 does not have. The soldier is no longer an R15 rig
-- wearing a mesh: it is the actual bind pose of the SWAT operator model, so
-- there are clavicles, two spine segments and toes to drive. See HumanPose.
--
-- Angles are radians in CHARACTER space (+X right, +Y up, +Z back), converted
-- per bone by HumanRig.anatomical.
--------------------------------------------------------------------------------
C.HUMAN_ANIM = {
	-- CLAVICLES. The single biggest reason an R15 soldier reads as a doll
	-- holding a prop is that its shoulders cannot move. A real shooter rolls
	-- both shoulders forward around the weapon and shrugs them up slightly;
	-- the firing side also pulls back to seat the stock in the pocket.
	CLAVICLE_FORWARD = math.rad(16),
	CLAVICLE_SHRUG = math.rad(7),
	CLAVICLE_BRACE = math.rad(6),
	CLAVICLE_SUPPORT_LIFT = math.rad(4),

	-- SPINE. Splitting the lean across two joints is what rounds the chest
	-- over the gun instead of hinging the whole trunk at the waist.
	SPINE_LOWER_SHARE = 0.38, -- fraction taken by the lower segment
	ADS_SPINE_FORWARD = math.rad(7),
	ADS_CHEST_ROUND = math.rad(9),
	CROUCH_SPINE_FORWARD = math.rad(6),
	ADS_HEAD_TILT = math.rad(4), -- cheek down onto the stock

	-- LEGS AND FEET.
	CROUCH_KNEE_HOLD = math.rad(52),
	CROUCH_HIP_RATIO = 0.62,
	STANCE_WIDTH = math.rad(6),
	PELVIS_ROLL = math.rad(3.2), -- weight transfer onto the loaded leg
	TOE_PUSH = math.rad(26), -- roll over the ball at push-off
	TOE_LIFT = math.rad(12), -- dorsiflex so the swing foot clears

	-- ARMS with no weapon in hand.
	IDLE_ARM_DROOP = math.rad(6),
	IDLE_ELBOW = math.rad(18),
	ARM_SWING = math.rad(15),

	-- ELBOW POLES, in character space. The support elbow drops under the
	-- handguard and tucks in as the weapon comes up; the firing elbow rides
	-- lower and closer to the ribs when braced.
	POLE_SUPPORT = Vector3.new(-0.62, -1.00, 0.26),
	POLE_SUPPORT_ADS = Vector3.new(-0.34, -1.00, 0.14),
	POLE_FIRING = Vector3.new(0.72, -0.90, 0.30),
	POLE_FIRING_ADS = Vector3.new(0.46, -1.00, 0.16),

	-- The aiming eye, relative to the Head bone. Offset to the dominant-eye
	-- side, because the rifle comes to one eye and the head comes over the
	-- stock to meet it -- not to the middle of the face.
	EYE_OFFSET = CFrame.new(0.085, 0.26, -0.30),
}

--------------------------------------------------------------------------------
-- ARM STAMINA
--
-- OPERATOR's stated reason for having a high/low ready system is that holding
-- a rifle up is WORK. Blacksite already had the poses and no reason to ever
-- use them. Fatigue climbs while the gun is up, falls while it is down, and
-- what it buys you is sway -- a tired shooter cannot hold a sight picture.
--------------------------------------------------------------------------------
C.ARM_STAMINA = {
	DRAIN_ADS = 1.0 / 26, -- full fatigue after 26 s on the sights
	DRAIN_HIP = 1.0 / 62, -- gun up but not aiming
	RECOVER_READY = 1.0 / 9, -- at low ready, back in 9 s
	RECOVER_SPRINT = 1.0 / 6,

	SAG = 0.30, -- radians of muzzle droop at full fatigue
	SAG_K = 26,
	SAG_D = 8.5,
	SWAY_MULT = 2.4, -- multiplies idle sway at full fatigue
	ADS_PENALTY = 0.55, -- fraction added to adsTime at full fatigue
}

C.ARMS = {
	-- Lengthened. The shoulders moved from 0.42 behind the CAMERA to 0.68
	-- behind the eye once they were anchored to the chest, so at the corrected
	-- HIP pose the old 1.20 total firing reach put the arm at 99% extension --
	-- dead straight, which looks like a mannequin. 1.46 lands it at 72%.
	FIRING_UPPER = 0.76,
	FIRING_FORE = 0.70,
	SUPPORT_UPPER = 1.55,
	SUPPORT_FORE = 1.45,

	-- TORSO-local now, not camera-local. Shoulders welded to the camera meant
	-- the whole arm structure was rigid to the eyeball -- turn your head and
	-- your shoulders turned with it, instantly and exactly. They hang off the
	-- chest, which lags, and that is where the arm motion comes from.
	SHOULDER_R = Vector3.new(0.42, -0.10, 0.20),
	SHOULDER_L = Vector3.new(-0.38, -0.08, 0.20),

	-- Which way the elbows break: down and outward.
	POLE_R = Vector3.new(0.80, -1.00, 0.20),
	POLE_L = Vector3.new(-0.80, -1.00, 0.20),
}

--------------------------------------------------------------------------------
-- WEAPON HANDLING TIMES (seconds)
-- These are the OPERATOR-flavoured bits: everything is a timed manual action.
--------------------------------------------------------------------------------
C.HANDLING = {
	-- Tuned toward the deliberate end. A tactical reload lands a shade over
	-- two seconds and an emergency one just under -- slow enough that choosing
	-- when to do it is a decision, fast enough that it is not a punishment.
	-- THESE WERE THE ANIMATION BUG.
	--
	-- Every timeline is stretched to the duration the caller asks for, and the
	-- caller asks for these -- so the animations were not badly keyframed so
	-- much as played at roughly HALF SPEED. The old numbers gave a 2.47 s
	-- tactical reload and a 1.47 s emergency. A trained shooter does a tactical
	-- in about 1.6 s and a speed reload in a shade over 1 s, and no amount of
	-- keyframe polish makes slow motion read as competence.
	--
	-- Tactical  = MAG_OUT + MAG_STOW + MAG_IN            = 1.24 s
	-- Emergency = MAG_OUT + MAG_IN                       = 0.86 s
	-- Empty     = MAG_OUT + MAG_IN + BOLT_RELEASE        = 1.08 s
	MAG_OUT = 0.34, -- rock the magazine out
	MAG_STOW = 0.38, -- extra time to put a partial mag back on the rig
	MAG_IN = 0.52, -- seat a fresh magazine
	BOLT_RELEASE = 0.22, -- slap the bolt release / rack the charging handle
	SHELL_LOAD = 0.38, -- one shotgun shell into the tube
	SHELL_START = 0.24, -- bringing the gun down to start loading shells
	PUMP_TIME = 0.42, -- full fore-end cycle after a shotgun shot
	PUMP_BACK = 0.17, -- ...of which this much is the rearward stroke
	CHECK_MAG = 0.62, -- pull the mag, eyeball the stack, seat it again
	CHECK_CHAMBER = 0.42, -- pinch-check the chamber
	SWAP_WEAPON = 0.62,
	FIRE_SELECTOR = 0.11,
	INSPECT = 2.10,
	BANDAGE = 4.50, -- pressure dressing, cannot be rushed
	INTERACT = 0.45,

	-- A tap on R is a tactical (retain) reload. Hold it past this to dump the
	-- mag on the floor -- faster, but that magazine is gone.
	EMERGENCY_HOLD = 0.28,
}

--------------------------------------------------------------------------------
-- DAMAGE / MEDICAL
-- No regeneration. Ever. You heal by using a dressing, and only to a cap.
--------------------------------------------------------------------------------
C.HEALTH = {
	MAX = 100,
	-- A pressure dressing restores only a tiny amount of immediate function. It
	-- controls one external wound; it does not repair fractures, organs or
	-- internal bleeding.
	DRESSING_HEAL = 4,
	BANDAGE_HEAL = 4, -- retained as an alias for older client code
	BANDAGE_START_COUNT = 2,
	ANATOMY_TICK = 0.25,
	BLOOD_MAX_ML = 5000,
	BLOOD_FATAL_ML = 1650,

	BLEED_DPS = 1.6, -- legacy fallback; Anatomy.luau owns active blood loss
	BLEED_CHANCE = 0.55, -- chance a hit opens a bleed
	BLEED_THRESHOLD = 12, -- ...only if the hit did at least this much

	-- Multiplier applied to a weapon's base damage per R15 limb.
	PARTS = {
		Head = 4.20,
		UpperTorso = 1.00,
		LowerTorso = 0.90,
		LeftUpperArm = 0.65,
		RightUpperArm = 0.65,
		LeftLowerArm = 0.55,
		RightLowerArm = 0.55,
		LeftHand = 0.45,
		RightHand = 0.45,
		LeftUpperLeg = 0.72,
		RightUpperLeg = 0.72,
		LeftLowerLeg = 0.60,
		RightLowerLeg = 0.60,
		LeftFoot = 0.45,
		RightFoot = 0.45,
		Torso = 0.95,
		["Left Arm"] = 0.60,
		["Right Arm"] = 0.60,
		["Left Leg"] = 0.65,
		["Right Leg"] = 0.65,
	},
	DEFAULT_PART_MULT = 0.85,

	-- Humanoid health remains the lethal pool. These independent trauma pools
	-- control function, allowing a surviving hit to damage a specific limb.
	LIMB_MAX = {
		Head = 35,
		Torso = 100,
		LeftArm = 65,
		RightArm = 65,
		LeftLeg = 75,
		RightLeg = 75,
	},
	LIMB_GROUPS = {
		Head = "Head",
		UpperTorso = "Torso", LowerTorso = "Torso", Torso = "Torso",
		LeftUpperArm = "LeftArm", LeftLowerArm = "LeftArm", LeftHand = "LeftArm",
		RightUpperArm = "RightArm", RightLowerArm = "RightArm", RightHand = "RightArm",
		LeftUpperLeg = "LeftLeg", LeftLowerLeg = "LeftLeg", LeftFoot = "LeftLeg",
		RightUpperLeg = "RightLeg", RightLowerLeg = "RightLeg", RightFoot = "RightLeg",
		["Left Arm"] = "LeftArm", ["Right Arm"] = "RightArm",
		["Left Leg"] = "LeftLeg", ["Right Leg"] = "RightLeg",
	},
	LIMB_BLEED_MULT = {
		Head = 1.30, Torso = 1.25,
		LeftArm = 0.90, RightArm = 0.90,
		LeftLeg = 1.05, RightLeg = 1.05,
	},
	LIMB_CRITICAL = 0.25,
	LIMB_WOUNDED = 0.50,
	LEG_SPEED_MIN = 0.38,
	SPRINT_LEG_MIN = 0.34,
	ARM_RECOIL_MAX = 1.55,
	ARM_SWAY_MAX = 1.90,
	ARM_RELOAD_MAX = 1.35,
	FIRST_PERSON_BODY_ALPHA = 0.02,
}

--------------------------------------------------------------------------------
-- BALLISTICS
--------------------------------------------------------------------------------
C.BALLISTICS = {
	MAX_RANGE = m(180),
	-- Damage falls off from 100% at POINT_BLANK to FALLOFF_MIN at MAX_RANGE.
	FALLOFF_START = m(35),
	FALLOFF_MIN = 0.55,

	-- Wall penetration: a round loses this fraction of its damage per stud of
	-- material it passes through, and stops after PEN_MAX_LAYERS surfaces.
	PEN_LOSS_PER_STUD = 0.30,
	PEN_MAX_LAYERS = 2,
	PEN_MAX_THICKNESS = 2.6, -- studs of material a rifle round can defeat

	-- Four ball rounds followed by one tracer is a common combat load. The
	-- server remains hitscan; this is only the short projectile streak the eye
	-- sees after the shot has already been resolved.
	TRACER_EVERY = 5,
	TRACER_SPEED = m(850),
	TRACER_LENGTH_MIN = 3.2,
	TRACER_LENGTH_MAX = 6.8,
	TRACER_WIDTH_NEAR = 0.022,
	TRACER_WIDTH_FAR = 0.010,
	TRACER_FADE_DISTANCE = m(120),
	TRACER_LIFETIME = 0.35,

	IMPACT_LIFETIME = 22, -- seconds bullet holes stay
}

--------------------------------------------------------------------------------
-- NOISE -- the entity hunts what it hears. Radius in studs.
--------------------------------------------------------------------------------
C.NOISE = {
	GUNSHOT = m(120),
	GUNSHOT_SUPPRESSED = m(26),
	IMPACT = m(14), -- where the round lands, not where it was fired
	SPRINT = m(22),
	WALK = m(11),
	SLOW = m(3.5),
	CROUCH = m(4.5),
	LAND = m(16),
	DOOR = m(15),
	MAG_DROP = m(9),
	RELOAD = m(5),
	OBJECTIVE = m(45), -- picking up a fuse rattles the whole rack
	POWER_ON = m(400), -- the generator waking up is heard everywhere

	SAMPLE_RATE = 0.30, -- how often the server samples player movement noise
}

--------------------------------------------------------------------------------
-- THE ENTITY -- SUBJECT 09
-- It cannot be killed. It can be hurt enough to make it leave, for a while.
--------------------------------------------------------------------------------
C.ENTITY = {
	NAME = "SUBJECT_09",

	HEIGHT = 7.4, -- studs -- deliberately taller than a door feels comfortable
	HIP_HEIGHT = 3.6,

	SPEED_IDLE = m(1.5),
	SPEED_INVESTIGATE = m(3.0),
	SPEED_HUNT = m(6.6), -- faster than your sprint. You do not outrun it.
	SPEED_CHARGE = m(8.2),

	-- Perception
	HEAR_MULT = 1.0, -- global scalar on every NOISE radius
	SIGHT_RANGE_DARK = m(9), -- it sees you in blackness at knife range
	SIGHT_RANGE_LIT = m(38), -- ...and across a lit room
	SIGHT_RANGE_LIGHT_ON = m(70), -- your torch is a beacon
	SIGHT_FOV = math.rad(115),

	ATTACK_RANGE = 7.0,
	ATTACK_DAMAGE = 62, -- two hits kill a healthy operator
	ATTACK_COOLDOWN = 1.35,
	ATTACK_WINDUP = 0.30,

	-- Not killable, but suppressible.
	POISE = 340, -- damage inside POISE_WINDOW needed to drive it off
	POISE_WINDOW = 4.0,
	FLEE_TIME = 34.0, -- seconds it stays gone after being driven off
	FLEE_SPEED = m(9.0),

	SEARCH_TIME = 14.0, -- how long it pokes around a noise before giving up
	HUNT_MEMORY = 7.5, -- seconds it keeps chasing after losing you
	REPATH = 0.55, -- seconds between path recomputes while hunting

	-- After the generator comes online it stops being patient.
	POWERED_AGGRESSION = 1.45, -- multiplies speed and hearing
}

--------------------------------------------------------------------------------
-- MAP GENERATION
--------------------------------------------------------------------------------
C.MAP = {
	SEED = 0, -- 0 = random per server. Set a number to lock the layout.

	CELL = 8, -- studs per grid cell (one cell = a 2.8 m wide corridor)
	GRID_W = 56,
	GRID_H = 48,

	WALL_HEIGHT = 11,
	CEILING_THICK = 2,
	FLOOR_THICK = 4,

	ROOM_TRIES = 180,
	ROOM_COUNT = 15,
	ROOM_MIN = 5, -- cells
	ROOM_MAX = 11,
	ROOM_MARGIN = 2, -- empty cells kept between rooms

	EXTRA_LOOPS = 4, -- extra corridors so the map is not a dead-end tree

	FUSE_COUNT = 3,

	LIGHT_CORRIDOR_EVERY = 5, -- cells
	EMERGENCY_LIGHT_CHANCE = 0.22, -- red lights that work before power is on
	FLICKER_CHANCE = 0.30,

	-- Ordinary working ceiling lights.
	--
	-- These went 0.85 -> 6.0 across four passes and the room never got
	-- brighter, because Brightness was the wrong knob. Per the docs,
	-- "Brightness is still limited to the light's defined range, so a higher
	-- Brightness value doesn't light up a larger region around the light" --
	-- it saturates the core of the pool and does nothing to the edge, so past
	-- about 3 you are only making the hotspot whiter. Meanwhile Range 75 and
	-- Angle 120 spread that fixed energy across an enormous cone, so the pool
	-- was wide, flat and faint everywhere.
	--
	-- Actual brightness now comes from ATMO.EXPOSURE (a real multiplier on the
	-- whole frame) and from GFX no longer crushing the shadows to zero. These
	-- go back to physically sane values that produce a POOL with an edge.
	--
	-- Note also: the engine range limit was 60 studs until it was doubled to
	-- 120 -- on an older client, Range 75 was being silently clamped to 60.
	LIGHT_BRIGHTNESS = 3.2,
	LIGHT_RANGE = 42,
	LIGHT_ANGLE = 100,
	LIGHT_COLOR = Color3.fromRGB(255, 228, 182), -- warm sodium, not white
	LIGHT_TUBE_COLOR = Color3.fromRGB(255, 236, 205),

	EMERGENCY_BRIGHTNESS = 0.55,
	EMERGENCY_RANGE = 16,
	EMERGENCY_COLOR = Color3.fromRGB(255, 62, 42),

	-- Most fixtures are simply working. Restoring power brings up the rest and
	-- is still worth doing, but the facility is lit and navigable from the
	-- start. Drop PRELIT_CHANCE toward 0 for a dark run.
	PRELIT_CHANCE = 0.85,
	PRELIT_DIM = 1.0,

	CLUTTER_PER_ROOM = 5,
}

--------------------------------------------------------------------------------
-- ARENA -- the daylight PVP test box. See ArenaBuilder.
--
-- Cover heights are chosen against the actual rig: standing eye is ~5.5 studs
-- off the deck, crouched eye ~4.1. So 2.6 is shoot-over-it, 4.6 is the one that
-- makes analog crouch worth having, 5.6 forces you to lean, 7.4 is full cover.
--------------------------------------------------------------------------------
C.ARENA = {
	RESPAWN = 2.5, -- fast: this is a test box, not a punishment
	SIZE = 320, -- studs square. 112 m -- big enough for the 100 m plate.
	WALL_HEIGHT = 26,
	PAD = 150, -- concrete apron in the middle

	BUILDING = 46, -- centre building footprint
	DOOR_WIDTH = 6, -- narrow on purpose: this is what tests weapon collision
	PLATFORM_HEIGHT = 9,

	-- Plate distances in METRES. Studs = metres * STUDS_PER_METER.
	RANGE_MARKS = { 5, 10, 25, 50, 75, 100 },

	-- {x, z, facing degrees}. Opposed pairs, plus two flankers so a
	-- three-or-four-way free-for-all is not always the same two lanes.
	SPAWNS = {
		{ 0, 132, 180 },
		{ 0, -132, 0 },
		{ 132, 0, 270 },
		{ -132, 0, 90 },
	},
}

--------------------------------------------------------------------------------
-- ATMOSPHERE
--------------------------------------------------------------------------------
C.ATMO = {
	-- Not pure black. At (2,2,3) anything outside a light pool was a void with
	-- no readable geometry at all, which reads as broken rather than dark.
	AMBIENT_DARK = Color3.fromRGB(24, 26, 31),
	AMBIENT_POWERED = Color3.fromRGB(44, 47, 54),
	-- Not zero. The docs are explicit that the EFFECTIVE OutdoorAmbient is
	-- max(OutdoorAmbient, Ambient) per channel -- setting it to black does not
	-- darken anything, it just means Ambient's hue leaks into any part of the
	-- map that can see sky. Matching it to Ambient is the honest way to say
	-- "there is no outdoors here".
	OUTDOOR_AMBIENT = Color3.fromRGB(24, 26, 31),

	-- Lighting.Brightness is the SUN AND MOON ONLY -- it has never had any
	-- effect on PointLights or SpotLights, so setting it to 0 was not what was
	-- making the facility dark, and raising it would not have helped either.
	BRIGHTNESS = 0,

	-- THIS is the master gain, and it was sitting at 0 the whole time.
	-- ExposureCompensation is a pre-tonemap exposure bias in STOPS: +1 is
	-- twice as much light reaching the tonemapper, -1 is half, range -5..5.
	-- One stop here does more for perceived brightness than any change you can
	-- make to an individual fixture, because it multiplies the light that is
	-- already there instead of adding light that is not.
	EXPOSURE = 1.15, -- 2^1.15 = 2.2x
	EXPOSURE_POWERED = 0.55, -- pull back once the mains are up, or it blows out

	----------------------------------------------------------------------------
	-- DAYLIGHT, for ARENA mode. Everything above is tuned for a windowless
	-- concrete box lit by three working fixtures; none of it survives contact
	-- with the sun, so this is a complete separate set rather than a scale on
	-- the dark one.
	----------------------------------------------------------------------------
	DAY = {
		CLOCK = 14.4, -- mid-afternoon: long enough shadows to read geometry
		GEO_LATITUDE = 24,
		BRIGHTNESS = 2.6, -- this one IS the sun, so here it matters
		EXPOSURE = 0, -- no gain needed; there is plenty of light
		AMBIENT = Color3.fromRGB(112, 118, 130), -- shadow fill, cool sky bounce
		OUTDOOR_AMBIENT = Color3.fromRGB(140, 148, 162),
		FOG_COLOR = Color3.fromRGB(178, 190, 206),
		FOG_START = 220,
		FOG_END = 1400, -- basically off; it is only there for depth on the walls
		ENV_DIFFUSE = 0.40, -- sky-derived fill. Correct outdoors, wrong indoors.
		ENV_SPECULAR = 0.30,

		-- The cold, crushed horror grade looks like a broken TV in daylight.
		GRADE_BRIGHTNESS = 0.0,
		GRADE_CONTRAST = 0.06,
		GRADE_SATURATION = -0.06,
		GRADE_TINT = Color3.fromRGB(255, 252, 246),

		ATMO_DENSITY = 0.30,
		ATMO_HAZE = 0.9,
		ATMO_GLARE = 0.25,
		ATMO_COLOR = Color3.fromRGB(199, 212, 228),
		ATMO_DECAY = Color3.fromRGB(106, 118, 136),
	},
	FOG_END = m(46),
	FOG_END_POWERED = m(80),
	FOG_COLOR = Color3.fromRGB(4, 5, 7),

	TORCH_BRIGHTNESS = 3.6,
	TORCH_RANGE = m(30),
	TORCH_ANGLE = 42,
	TORCH_COLOR = Color3.fromRGB(255, 244, 214),
	TORCH_BATTERY = 260, -- seconds of continuous use
	TORCH_BATTERY_WARN = 45,

	NVG_BATTERY = 190,

	LASER_RANGE = m(60),
	LASER_COLOR = Color3.fromRGB(255, 40, 40),

	-- Fear response: heartbeat + vignette when the entity is close.
	FEAR_RANGE = m(30),
	FEAR_RANGE_LOS = m(55),
	FEAR_ATTACK = 3.0, -- how fast dread climbs
	FEAR_DECAY = 0.55,
}

--------------------------------------------------------------------------------
-- NIGHT VISION -- panoramic four-tube field.
--
-- Roblox post-processing is full-screen: there is no way to apply a
-- ColorCorrection to a circle. So the amplified image is rendered over the
-- whole screen and everything *outside* the tubes is covered by a mask built
-- from horizontal strips -- for each strip we work out the span the two
-- circles cover and darken what is left. The strips are hard-edged, which
-- would normally stair-step, except that real tubes vignette hard at the rim
-- and the vignette rings drawn inside each tube hide the artifact completely.
--
-- Blur is deliberately global. Real tubes resolve about 20/40, so the whole
-- image is soft -- and it means the barely-visible world outside the tubes is
-- soft too, which is what you see when your eye is not behind glass.
--
-- The scanline union solver supports any tube count; this pass uses four.
--------------------------------------------------------------------------------
C.NVG = {
	-- Circle centres and radius as a fraction of screen HEIGHT, measured from
	-- screen centre. Negative X is left. The inner pair carries the central field:
	-- the circles are large, set well apart, and only just touch at the
	-- centre line. The mask solver merges however many spans each scanline
	-- produces and darkens the complement, so the wedge of black that opens
	-- up between the tubes above and below the meeting point is handled
	-- correctly. That wedge is a real feature of the look.
	-- SIZE. r = 0.232 was not arbitrary -- it is the geometrically correct
	-- apparent size for a 40 degree tube FOV at a 78 degree vertical camera FOV:
	--     r/h = tan(20) / (2*tan(39)) = 0.364 / 1.620 = 0.225
	-- The problem is that this is a 2.26:1 ultrawide viewport. Vertical FOV is
	-- fixed, so a wide window shows you far MORE horizontal world, and a
	-- correctly-sized circle ends up a small island in the middle of it.
	--
	-- Real goggles do not behave like that, because the housing sits against
	-- your face -- you cannot see 130 degrees of room around the tubes, you see
	-- the inside of the mount. So the honest fix is to make the tubes big enough
	-- to dominate the frame the way a mounted set does, and let the mask be
	-- almost everything else. r = 0.36 with the centres pulled in to +/-0.215
	-- gives a 1.15h wide by 0.72h tall binocular field with a broad shared lens
	-- region -- which is also what actual dual-tube footage looks like once the
	-- IPD is set anywhere near correctly.
	-- THE TWO INNER CIRCLES. Each tube draws its own set of concentric rim
	-- rings, and once the circles genuinely overlapped, tube A's rim arcs were
	-- being drawn straight across tube B's image and vice versa. That is the
	-- dark lens shape in the middle with its own rings around it -- not a
	-- second pair of tubes, just two rim vignettes crossing each other.
	--
	-- Roblox UI has no way to clip a ring to another shape, so the fix is
	-- geometric: keep the tubes big but make them TANGENT rather than
	-- overlapping. Centres 0.704 apart against a radius sum of 0.716 leaves a
	-- 0.012 kiss -- enough that there is no seam at the meeting point, small
	-- enough that neither rim reaches into the other's field. The wedge of black
	-- that opens above and below the tangent point is a real feature of the look
	-- and the mask solver already handles it.
	-- CONNECTED, because that is how two eyes work. With dual tubes set to your
	-- actual IPD your brain FUSES the two fields into one -- you do not see two
	-- circles, you see a single roughly-circular field with slight lobes at the
	-- sides. The twin-circle look every game uses is a stylistic convention, not
	-- optics.
	--
	-- Tangent circles (the previous fix) were a workaround for the per-tube rim
	-- rings crossing into each other. Those rings are GONE now -- the vignette
	-- is baked into the mask itself and follows the union of the circles, so the
	-- tubes are free to overlap as hard as they like. See FEATHER_PASSES.
	-- OPERATOR-style panoramic field: two larger inner tubes carry most of the
	-- image while two slightly smaller outer tubes create the side lobes. The
	-- scanline union solver makes this a single soft field instead of four hard
	-- circles, and the slight Y offsets retain the characteristic scalloped top
	-- and bottom silhouette.
	TUBES = {
		{ x = -0.46, y = 0.025, r = 0.33 },
		{ x = -0.17, y = -0.010, r = 0.385 },
		{ x = 0.17, y = -0.010, r = 0.385 },
		{ x = 0.46, y = 0.025, r = 0.33 },
	},

	-- UNION-CORRECT FEATHER, replacing the ring vignette.
	--
	-- The mask solver already computes the union of the circles per scanline and
	-- darkens the complement. So: run that solve several times at shrinking
	-- radii and stack the complements at a fraction of the opacity each. Outside
	-- the largest radius every pass covers, giving full opacity; between the
	-- smallest and largest, progressively fewer passes reach, giving a gradient
	-- that follows the union boundary EXACTLY.
	--
	-- That is what makes the two tubes read as one connected field instead of
	-- two circles with a seam: there is no per-tube geometry left to give them
	-- away, and no rings to cross.
	FEATHER_PASSES = 8,
	FEATHER_DEPTH = 0.15, -- innermost pass is r * (1 - this)

	STRIPS = 144, -- mask resolution
	-- 0.90 let 10% of a now much brighter world through and the surround read
	-- as navy rather than as the inside of a helmet.
	MASK_OPACITY = 0.992,
	MASK_COLOR = Color3.fromRGB(2, 3, 4),

	-- Rim feathering. Rings must OVERLAP or you get a bullseye. They cover the
	-- outer band only, densely, so the strokes blend into a gradient.
	-- 0 disables the ring vignette entirely. The rings were per-tube, so once the
	-- circles overlapped, tube A's rim arcs were drawn straight across tube B's
	-- image -- the "two inner circles" report. FEATHER_PASSES does the job
	-- correctly and cannot produce that artifact because it works off the union.
	VIGNETTE_RINGS = 0,
	VIGNETTE_INNER = 0.72,
	VIGNETTE_DEPTH = 0.97,
	-- DELETED, not dimmed. A bright rim ring is the single most obviously-fake
	-- thing you can draw on a tube and it was still visible in the screenshot
	-- as a distinct cyan circle. Real tubes just fall off into black, so the
	-- outermost ring is now the same colour as every other one.

	----------------------------------------------------------------------------
	-- THE IMAGE.
	--
	-- The old version tried to make "amplified" out of ColorCorrection
	-- Brightness, which is ADDITIVE. Additive is a black-point lift: it turns
	-- an unlit room into a uniform glowing teal wash and it cannot make a dark
	-- SURFACE brighter than the empty air next to it, because it raises both
	-- by the same amount. That is why every value was wrong in both
	-- directions at once.
	--
	-- Gain is MULTIPLICATIVE, and Roblox has exactly one multiplicative
	-- pre-tonemap control: Lighting.ExposureCompensation, in stops. +1 is
	-- twice the light, +2 is four times, and it preserves true black exactly
	-- because 0 * 4 is still 0. That is the physics of an intensifier tube's
	-- gain stage, and it is the whole answer to "how do you sell amplified
	-- without lifting the black point".
	--
	-- The second half of the answer is that a real tube has terrible dynamic
	-- range. Everything it can see gets squashed into a narrow band around
	-- mid grey, so NVG needs NEGATIVE contrast. The old +0.20 was pushing the
	-- image apart at exactly the moment it should have been collapsing it.
	----------------------------------------------------------------------------
	EXPOSURE = 2.35, -- stops of gain. 2^2.70 = 6.5x the light.
	GAIN = 0.008, -- additive lift, now almost nothing. Deliberate.
	CONTRAST = -0.16, -- NEGATIVE, but -0.34 was flattening it into a teal wash
	TINT = Color3.fromRGB(124, 218, 220), -- white phosphor reads cyan, not green
	-- A tube picks up skyglow that your naked eye cannot -- but AMBIENT is a
	-- FLAT, shadowless, directionless fill, so every stud of it is contrast you
	-- can never get back. At (38,44,44) with +2.35 stops on top the tube
	-- interiors came out as a uniform teal sheet with no geometry in them,
	-- which is what the screenshot shows. Cut it hard and let EXPOSURE amplify
	-- the light that is actually in the room: then surfaces near a fixture or
	-- your own torch read bright and unlit corners stay properly dark, which is
	-- the whole point of wearing the things.
	AMBIENT = Color3.fromRGB(11, 14, 14),
	BLUR = 1.8, -- tubes resolve about 20/40. Softer than this reads as fog.

	-- AUTO-GATING. Point a real tube at a light and it protects itself: gain
	-- collapses in about 40 ms and takes a couple of seconds to come back.
	-- There is no way to read the framebuffer, so this is driven off actual
	-- light sources near and in front of the player, which is the same signal
	-- the tube is responding to anyway.
	GATE_RANGE = 34, -- studs. Lights inside this can gate the tubes.
	-- Was 1.9, which left daylight NVG at +0.95 stops -- still bright enough to
	-- fight with, so the gate read as a tint rather than as protection. At 2.8
	-- a full gate takes the tubes below unity and they become useless, which is
	-- the correct answer to wearing them at two in the afternoon.
	GATE_STRENGTH = 2.8, -- stops pulled off at full gate
	GATE_ATTACK = 22, -- how fast gain collapses
	GATE_RELEASE = 0.85, -- ...and how slowly it recovers
	GATE_TORCH = 0.75, -- your own weapon light gates you this hard
	-- Wearing tubes in daylight nearly shuts them. This is correct behaviour,
	-- not a bug: it is what happens if you flip PVS-31s down at two in the
	-- afternoon, and it is the cheapest possible demo that the gate works.
	GATE_DAYLIGHT = 0.92,

	-- Gain blows highlights out. A porch light through tubes is a white disc,
	-- not a bright lamp, so bloom gets much more aggressive while NVG is down.
	BLOOM_THRESHOLD = 0.66,
	BLOOM_INTENSITY = 1.25,
	BLOOM_SIZE = 26,

	-- SCINTILLATION. The old version jittered a global Brightness, which is a
	-- strobe, not noise -- the whole frame going light and dark together reads
	-- as a failing lamp. Real scintillation is SPATIAL: individual photon
	-- events sparkling at random points. A coarse grid of cells whose
	-- transparency re-rolls at a fixed rate costs nothing and is the single
	-- biggest tell between real NVG footage and a green filter.
	-- Trying to make ONE grid do both the fibre bundle and the scintillation was
	-- a mistake. Sizing it for an affordable number of updatable cells gave 20
	-- columns across a 1000 px region -- 50 px per cell -- and the result read
	-- as bubble wrap laid over the image, which is the single most obviously
	-- wrong thing in the screenshot. The two artifacts have opposite
	-- requirements and they are separate systems now:
	--
	--   FIBRE    dense, tiny, faint, and STATIC. Built once, never written to,
	--            so it can afford to be a thousand cells.
	--   SPARKLE  sparse, bright, and MOVING. A small pool of dots teleported to
	--            new random positions each roll, which is what photon events
	--            actually look like -- a few bright points appearing and going,
	--            not a grid modulating its brightness.
	-- OFF BY DEFAULT, and this is a judgement call rather than a bug fix.
	--
	-- To be invisible-but-present the bundle needs ~12 px cells, and over a tube
	-- field this size that is 4000+ Frames. Capping the count instead widens the
	-- pitch, and at 20 px the cells stop being a texture and become legible
	-- circles -- the hex dot pattern all over the last screenshot. There is no
	-- setting where this is both cheap and convincing, so it is off: the
	-- scintillation carries the "this is a tube" read on its own for 130
	-- instances instead of 1400.
	--
	-- Turn it on if you ever want it, but raise FIBRE_MAX with it or it will
	-- look like polka dots again.
	FIBRE = false,
	FIBRE_PITCH = 13, -- px between fibre cell centres at any resolution
	FIBRE_MAX = 3200, -- hard cap; pitch is widened rather than exceed this
	FIBRE_OPACITY = 0.020,

	SPARKLE_COUNT = 72,
	SPARKLE_SIZE = 2, -- px
	SPARKLE_RATE = 18, -- re-rolls per second, framerate independent
	SPARKLE_OPACITY = 0.18,
	SCINT_COLOR = Color3.fromRGB(182, 244, 238),

	-- TUBE-TO-TUBE VARIANCE. No two intensifiers are matched -- one is always
	-- a little hotter than the other, and your eye notices when they are
	-- identical. Post-processing cannot be masked per tube, but a nearly
	-- transparent tinted disc INSIDE each circle can, so the bias goes there.
	TUBE_BIAS = { -0.010, 0.020, -0.014, 0.012 },
}

--------------------------------------------------------------------------------
-- SCREEN EFFECTS
--
-- Three separate events, deliberately not one shared "damage" effect, because
-- they mean different things and the player has to be able to tell them apart
-- without a HUD:
--
--   FIRE      your own muzzle blast. Short, bright, and it accumulates into
--             DEAFEN if you keep the trigger down -- which is the honest cost
--             of full-auto in a building.
--   SUPPRESS  a round went past close enough to crack. Blur and desaturation,
--             no red -- because you have NOT been hit and the effect must never
--             be mistaken for damage. This is what makes incoming fire
--             frightening rather than merely informational.
--   HIT       you took it. Red, heavy, and slowest to clear.
--
-- Decay times are in seconds to fall from 1 to 0.
--------------------------------------------------------------------------------
-- ALL OF THESE WERE ROUGHLY TRIPLED after "the effects are too subtle".
--
-- I had been tuning them as garnish. ARMA's suppression is not garnish -- it
-- greys the world out, shoves your aim and makes you physically unable to hold
-- a sight picture, and that is the point: it is a weapon you use on someone
-- without hitting them. Being shot at should be the most violent thing that
-- happens to the screen short of being hit.
--
-- The FOV punch and the vignette are new. Vignette especially: closing the
-- edges of the frame is what makes suppression read as tunnel vision rather
-- than as a blur filter, and it is the single strongest cue available.
C.SCREENFX = {
	-- FIRE was still far too polite. A 0.19 s decay on a squared value means the
	-- effect is over in about 90 ms of visible time, which at a 780 rpm cycle
	-- rate is gone before the next round leaves. Slowed the decay and roughly
	-- doubled everything again -- firing should be violent on the screen, and
	-- the FOV punch is the part you feel rather than see.
	-- V17 OPERATOR reference: one short warm pressure pulse, not a full-screen
	-- concussion filter. Most of the event is weapon recoil and muzzle flame.
	FIRE_PULSE = 1.0,
	FIRE_DECAY = 0.12,
	FIRE_BLUR = 4.6,
	FIRE_BLOOM = 1.05,
	FIRE_FOV = 0.90,
	FIRE_VIGNETTE = 0.045,
	FIRE_SHAKE = 0.22,
	FIRE_TINT = Color3.fromRGB(255, 186, 145),
	FIRE_TINT_STRENGTH = 0.11,

	SUPPRESS_DECAY = 2.6,
	SUPPRESS_BLUR = 46.0,
	SUPPRESS_DESAT = 1.2, -- past monochrome: inverts slightly, which reads as shock
	SUPPRESS_CONTRAST = 0.5,
	SUPPRESS_VIGNETTE = 0.9, -- tunnel vision. This is the one that sells it.
	SUPPRESS_FOV = -5.5, -- FOV pulls IN: the world closes on you
	SUPPRESS_RANGE = 9.0, -- studs from the round's path that counts as close
	SUPPRESS_SHAKE = 2.4, -- rad/s into the head spring -- a real flinch
	SUPPRESS_WEAPON_KICK = 1.5, -- and the weapon is jolted too

	HIT_DECAY = 3.2,
	HIT_BLUR = 52.0,
	HIT_DESAT = 0.9,
	HIT_VIGNETTE = 0.85,
	HIT_FOV = -4.5,
	HIT_TINT = Color3.fromRGB(255, 138, 138),
	HIT_SHAKE = 3.4,

	-- Deafness: the low-frequency muffle after sustained fire.
	DEAFEN_PER_SHOT = 0.22,
	DEAFEN_DECAY = 4.0,
	DEAFEN_MUFFLE = 0.78, -- fraction of world volume removed at full deafness
}

--------------------------------------------------------------------------------
-- MUZZLE EFFECTS
--
-- The flash was one neon ball scaled randomly. A real one is three things that
-- look nothing alike and last very different lengths of time:
--
--   FLASH   the burning powder. 2-3 frames, blindingly bright, and CROSS-SHAPED
--           because it exits through the brake ports as well as the bore. The
--           random ROLL per shot is what stops repeated fire looking like a
--           blinking light -- it is the single cheapest trick here.
--   SMOKE   lingers for a second or two, drifts, expands, and is the only part
--           that accumulates. Sustained fire should leave you looking through
--           your own smoke, which is a real cost of shooting from cover.
--   BRASS   a case, spinning, ejected right and slightly back. Nothing sells
--           "a machine just cycled" like debris leaving it.
--------------------------------------------------------------------------------
C.MUZZLE = {
	-- Daylight-visible but not bloom-saturated. The previous 0.62-stud neon ball
	-- plus a brightness-68 light produced the giant white orb in the screenshots.
	-- This is a narrow directional gas flame lasting roughly one rendered frame.
	-- The previous 18 ms pulse could begin and end between rendered frames.
	-- This persists long enough to be visible while staying far smaller than the
	-- original white orb.
	-- Compact brake-port flash from the supplied OPERATOR shooting clip: a
	-- white-hot kernel, short red-orange tongues, one or two rendered frames.
	FLASH_CORE_TIME = 0.036,
	FLASH_AFTER_TIME = 0.052,
	FLASH_CORE_THICK = 0.050,
	FLASH_CORE_LENGTH = 0.22,
	FLASH_GAS_THICK = 0.078,
	FLASH_GAS_LENGTH = 0.31,
	FLASH_LOBES = 4,
	FLASH_LOBE_MIN = 0.17,
	FLASH_LOBE_MAX = 0.43,
	FLASH_LOBE_THICK = 0.045,
	FLASH_CONE = math.rad(10.5),
	FLASH_CORE_COLOR = Color3.fromRGB(255, 235, 200),
	FLASH_OUTER_COLOR = Color3.fromRGB(255, 82, 30),
	FLASH_LIGHT = 3.2,
	FLASH_LIGHT_RANGE = 7.0,
	FLASH_CHANCE = 1.00,
	SUPPRESSED_FLASH_CHANCE = 0.08,

	-- Built-in assets only; no copied game asset is embedded.
	FLASH_TEXTURE = "rbxasset://textures/particles/fire_main.dds",
	SMOKE_TEXTURE = "rbxasset://textures/particles/smoke_main.dds",

	-- A single short pressure puff, not a grey cloud attached to every bullet.
	BLAST_SMOKE_MIN = 1,
	BLAST_SMOKE_MAX = 2,
	BLAST_SMOKE_LIFE_MIN = 0.18,
	BLAST_SMOKE_LIFE_MAX = 0.34,
	BLAST_SMOKE_SPEED_MIN = 4.2,
	BLAST_SMOKE_SPEED_MAX = 7.8,
	BLAST_SMOKE_SIZE = 0.070,
	BLAST_SMOKE_GROW = 5.2,

	-- Lingering smoke is intentionally sparse. Most rifle shots make no obvious
	-- cloud in daylight; strings of fire build a faint veil through accumulation.
	LINGER_SMOKE_MIN = 0,
	LINGER_SMOKE_MAX = 1,
	LINGER_SMOKE_LIFE_MIN = 0.65,
	LINGER_SMOKE_LIFE_MAX = 1.25,
	LINGER_SMOKE_SPEED_MIN = 0.35,
	LINGER_SMOKE_SPEED_MAX = 1.15,
	LINGER_SMOKE_SIZE = 0.085,
	LINGER_SMOKE_GROW = 4.8,
	SMOKE_DRIFT = 0.72,

	-- Heat smoke only becomes visible after a real string, and remains thin.
	HEAT_PER_SHOT = 0.085,
	HEAT_COOL = 0.19,
	HEAT_THRESHOLD = 0.62,
	HEAT_SMOKE_RATE = 3.8,
	HEAT_SMOKE_LIFE_MIN = 0.72,
	HEAT_SMOKE_LIFE_MAX = 1.40,
	HEAT_SMOKE_SPEED_MIN = 0.12,
	HEAT_SMOKE_SPEED_MAX = 0.42,
	HEAT_SMOKE_SIZE = 0.055,
	HEAT_SMOKE_GROW = 4.0,

	BRASS_LIFE = 2.6,
	BRASS_SPEED = 12.0,
	BRASS_SPIN = 34.0,
	BRASS_SIZE = Vector3.new(0.055, 0.055, 0.14),
	BRASS_COLOR = Color3.fromRGB(196, 152, 66),

	-- Suppressors almost eliminate visible flame but retain more slow gas.
	SUPPRESSED_FLASH = 0.34,
	SUPPRESSED_BLAST_SMOKE = 0.55,
	SUPPRESSED_LINGER_SMOKE = 1.45,
	SUPPRESSED_HEAT = 1.30,
}


--------------------------------------------------------------------------------
-- GRAPHICS
--------------------------------------------------------------------------------
C.GFX = {
	-- Threshold above 1 means only actual light sources bloom, not pale walls.
	BLOOM_INTENSITY = 0.55,
	BLOOM_THRESHOLD = 1.15,
	BLOOM_SIZE = 22,

	-- Shallow focus close in. Subtle -- enough to keep the far dark reading as
	-- depth rather than as a flat black wall.
	DOF_FAR_INTENSITY = 0.14,
	DOF_FOCUS = 26,
	DOF_INFOCUS = 55,
	DOF_NEAR_INTENSITY = 0.06,

	----------------------------------------------------------------------------
	-- THE GRADE WAS EATING THE GAME.
	--
	-- ColorCorrectionEffect.Contrast pivots about 0.5:
	--     out = (in - 0.5) * (1 + Contrast) + 0.5 + Brightness
	-- At the old Contrast 0.18 / Brightness -0.025 that is out = 1.18*in - 0.115,
	-- which hits zero at in = 0.0975. EVERY PIXEL BELOW 9.75% GREY WAS BEING
	-- CLAMPED TO LITERAL BLACK by the colour grade, after lighting, after
	-- tonemapping, unconditionally.
	--
	-- Ambient-lit concrete at Ambient (14,15,18) lands around 3%. So it did not
	-- matter how bright the fixtures were or how high the ambient went -- the
	-- space between the light pools was being multiplied into nothing on the
	-- way to the screen. That is the "only pools of light exist, the space
	-- between them is unreadable" bug, and it is also half of why the NVG
	-- tubes render black.
	--
	-- Now: out = 1.10*in + 0.020, zero at in = -0.018, i.e. nothing is
	-- clipped. Still contrasty, still cold, but shadow detail survives.
	----------------------------------------------------------------------------
	GRADE_BRIGHTNESS = 0.020, -- a LIFT, not a cut. Protects the shadow floor.
	GRADE_CONTRAST = 0.10,
	GRADE_SATURATION = -0.28,
	GRADE_TINT = Color3.fromRGB(212, 220, 232),
}

--------------------------------------------------------------------------------
-- SOUND
-- These rbxasset:// paths ship inside the Roblox client, so the place works
-- offline with zero uploads. They are stand-ins -- drop your own asset ids in
-- here (rbxassetid://NUMBER) and everything else keeps working.
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
-- LAYERING
--
-- I cannot upload audio, so a "realistic gunshot" has to be SYNTHESISED out of
-- the handful of sounds that ship inside the Roblox client. One file played at
-- one pitch will never sound like a rifle -- what makes a gunshot read is that
-- it is four separate events inside 40 ms:
--
--   CRACK   the supersonic transient. Very short, very bright, no body.
--   BODY    the muzzle blast. This is the loudest part and the lowest.
--   TAIL    the room. Arrives a few ms late and decays; this is what makes a
--           shot sound like it happened INDOORS rather than in a vacuum.
--   MECH    the action cycling. Metallic, quiet, latest of the four.
--
-- Any entry may carry a `layers` array. Every layer has its own id, volume,
-- pitch and delay. The top-level id/vol/pitch is kept on every entry as the
-- primary layer so nothing that reads `def.id` directly has to change.
--------------------------------------------------------------------------------
C.SFX = {
	-- Close weapon reports are deliberately dry and short. The environment
	-- mixer creates reflections and tails from room geometry; baking a cavern
	-- into the primary layer is what made the old shots sound like explosions.
	SHOT_RIFLE = {
		environmentAware = true,
		id = "rbxasset://sounds/pop_mid.wav",
		vol = 0.96,
		pitch = 0.82,
		layers = {
			{
				id = "rbxasset://sounds/snap.mp3", vol = 0.74, pitch = 1.86,
				role = "transient", pitchJitter = 0.018, randomVolume = 0.05,
				eq = { low = -10, mid = 1, high = 5.5 },
				compress = { attack = 0.004, threshold = -15, ratio = 5.5, release = 0.08, makeup = 0.5 },
			},
			{
				id = "rbxasset://sounds/pop_mid.wav", vol = 0.96, pitch = 0.82, delay = 0.002,
				role = "body", pitchJitter = 0.012, randomVolume = 0.035,
				eq = { low = 4.5, mid = 1.0, high = -5.5 },
				compress = { attack = 0.008, threshold = -12, ratio = 6.5, release = 0.14, makeup = 0.8 },
			},
			{
				id = "rbxasset://sounds/collide.wav", vol = 0.36, pitch = 0.47, delay = 0.004,
				role = "body", pitchJitter = 0.015,
				eq = { low = 6.0, mid = -2.5, high = -10.0 },
				compress = { attack = 0.010, threshold = -13, ratio = 4.5, release = 0.18, makeup = 0.3 },
			},
			{
				id = "rbxasset://sounds/snap.mp3", vol = 0.20, pitch = 1.22,
				role = "reflection", eq = { low = -7, mid = -1, high = 2.5 },
			},
			{
				id = "rbxasset://sounds/pop_mid.wav", vol = 0.13, pitch = 0.43,
				role = "tail", eq = { low = 2.0, mid = -5.0, high = -13.0 },
				minDistance = 14, maxDistance = 520,
			},
			{
				id = "rbxasset://sounds/switch3.wav", vol = 0.18, pitch = 1.08, delay = 0.026,
				role = "mechanism", pitchJitter = 0.025,
				eq = { low = -6, mid = 2.5, high = 0.5 },
			},
		},
	},
	SHOT_PISTOL = {
		environmentAware = true,
		id = "rbxasset://sounds/pop_mid.wav",
		vol = 0.80,
		pitch = 1.16,
		layers = {
			{
				id = "rbxasset://sounds/snap.mp3", vol = 0.68, pitch = 2.22,
				role = "transient", pitchJitter = 0.020, randomVolume = 0.06,
				eq = { low = -11, mid = 1.5, high = 6.0 },
				compress = { attack = 0.004, threshold = -15, ratio = 5, release = 0.07, makeup = 0.3 },
			},
			{
				id = "rbxasset://sounds/pop_mid.wav", vol = 0.80, pitch = 1.16, delay = 0.002,
				role = "body", pitchJitter = 0.015,
				eq = { low = 2.5, mid = 1.0, high = -4.5 },
				compress = { attack = 0.007, threshold = -13, ratio = 5.5, release = 0.12, makeup = 0.5 },
			},
			{
				id = "rbxasset://sounds/collide.wav", vol = 0.20, pitch = 0.68, delay = 0.003,
				role = "body", eq = { low = 3.0, mid = -2.5, high = -10.0 },
			},
			{
				id = "rbxasset://sounds/snap.mp3", vol = 0.16, pitch = 1.48,
				role = "reflection", eq = { low = -8, mid = -1, high = 3 },
			},
			{
				id = "rbxasset://sounds/pop_mid.wav", vol = 0.085, pitch = 0.58,
				role = "tail", eq = { low = 0, mid = -6, high = -14 },
				minDistance = 10, maxDistance = 340,
			},
			{
				id = "rbxasset://sounds/switch3.wav", vol = 0.26, pitch = 1.34, delay = 0.020,
				role = "mechanism", pitchJitter = 0.025,
				eq = { low = -7, mid = 3.0, high = 1.5 },
			},
		},
	},
	SHOT_SHOTGUN = {
		environmentAware = true,
		id = "rbxasset://sounds/pop_mid.wav",
		vol = 1.0,
		pitch = 0.60,
		layers = {
			{
				id = "rbxasset://sounds/snap.mp3", vol = 0.56, pitch = 1.34,
				role = "transient", pitchJitter = 0.018,
				eq = { low = -7, mid = 0, high = 4.0 },
			},
			{
				id = "rbxasset://sounds/pop_mid.wav", vol = 1.00, pitch = 0.60, delay = 0.003,
				role = "body", pitchJitter = 0.012,
				eq = { low = 7.0, mid = 0, high = -8.0 },
				compress = { attack = 0.008, threshold = -10, ratio = 7.5, release = 0.22, makeup = 0.8 },
			},
			{
				id = "rbxasset://sounds/collide.wav", vol = 0.58, pitch = 0.35, delay = 0.004,
				role = "body", eq = { low = 8, mid = -3, high = -12 },
			},
			{
				id = "rbxasset://sounds/snap.mp3", vol = 0.22, pitch = 0.92,
				role = "reflection", eq = { low = -4, mid = -2, high = 1 },
			},
			{
				id = "rbxasset://sounds/pop_mid.wav", vol = 0.20, pitch = 0.33,
				role = "tail", eq = { low = 4, mid = -6, high = -15 },
				minDistance = 18, maxDistance = 620,
			},
		},
	},
	SHOT_SUPPRESSED = {
		environmentAware = true,
		id = "rbxasset://sounds/pop_mid.wav",
		vol = 0.54,
		pitch = 0.54,
		layers = {
			{
				id = "rbxasset://sounds/pop_mid.wav", vol = 0.54, pitch = 0.54,
				role = "body", pitchJitter = 0.018, randomVolume = 0.04,
				eq = { low = 5.0, mid = -1.0, high = -8.0 },
				compress = { attack = 0.008, threshold = -16, ratio = 4.5, release = 0.15, makeup = 0.4 },
			},
			{
				id = "rbxasset://sounds/snap.mp3", vol = 0.18, pitch = 1.54, delay = 0.002,
				role = "transient", eq = { low = -12, mid = -2, high = 1.0 },
			},
			{
				id = "rbxasset://sounds/switch3.wav", vol = 0.34, pitch = 1.20, delay = 0.020,
				role = "mechanism", pitchJitter = 0.024,
				eq = { low = -5, mid = 3, high = 0 },
			},
			{
				id = "rbxasset://sounds/pop_mid.wav", vol = 0.045, pitch = 0.40,
				role = "tail", eq = { low = 1, mid = -8, high = -16 },
			},
		},
	},

	-- Locomotion is a heel/toe impact plus equipment motion, not one loud loop.
	STEP_HARD = {
		id = "rbxasset://sounds/action_footsteps_plastic.mp3",
		vol = 0.30,
		pitch = 0.68,
		layers = {
			{
				id = "rbxasset://sounds/collide.wav", vol = 0.20, pitch = 0.50,
				role = "foley", pitchJitter = 0.08, randomVolume = 0.14,
				eq = { low = 4, mid = -2, high = -8 },
			},
			{
				id = "rbxasset://sounds/action_footsteps_plastic.mp3", vol = 0.30, pitch = 0.68, delay = 0.010,
				role = "foley", pitchJitter = 0.06, randomVolume = 0.10,
				eq = { low = -1, mid = 1, high = -4 },
			},
		},
	},
	STEP_SOFT = {
		id = "rbxasset://sounds/action_footsteps_plastic.mp3",
		vol = 0.13,
		pitch = 0.56,
		role = "foley",
		eq = { low = 0, mid = -1, high = -7 },
	},

	-- Surface-specific footsteps. They share client-shipped source material but
	-- use different transients, resonances and tails, which is much more grounded
	-- than playing the same plastic footstep on concrete, steel and dirt.
	STEP_CONCRETE = {
		id = "rbxasset://sounds/action_footsteps_plastic.mp3", vol = 0.30, pitch = 0.66,
		layers = {
			{ id = "rbxasset://sounds/collide.wav", vol = 0.22, pitch = 0.48, role = "foley", pitchJitter = 0.07, randomVolume = 0.12, eq = { low = 4, mid = -1, high = -8 } },
			{ id = "rbxasset://sounds/action_footsteps_plastic.mp3", vol = 0.28, pitch = 0.69, delay = 0.010, role = "foley", pitchJitter = 0.055, eq = { low = 0, mid = 1, high = -5 } },
		},
	},
	STEP_METAL = {
		id = "rbxasset://sounds/switch3.wav", vol = 0.25, pitch = 0.62,
		layers = {
			{ id = "rbxasset://sounds/collide.wav", vol = 0.18, pitch = 0.58, role = "foley", pitchJitter = 0.08, eq = { low = 1, mid = 2, high = -4 } },
			{ id = "rbxasset://sounds/switch3.wav", vol = 0.25, pitch = 0.62, delay = 0.008, role = "reflection", pitchJitter = 0.06, eq = { low = -5, mid = 3, high = 1 }, reverb = { decay = 0.30, wet = -12, dry = -4 } },
		},
	},
	STEP_WOOD = {
		id = "rbxasset://sounds/collide.wav", vol = 0.25, pitch = 0.39,
		layers = {
			{ id = "rbxasset://sounds/collide.wav", vol = 0.25, pitch = 0.39, role = "foley", pitchJitter = 0.08, eq = { low = 6, mid = 0, high = -10 } },
			{ id = "rbxasset://sounds/pop_mid.wav", vol = 0.07, pitch = 0.34, delay = 0.012, role = "reflection", eq = { low = 4, mid = -5, high = -16 }, reverb = { decay = 0.22, wet = -16, dry = -6 } },
		},
	},
	STEP_DIRT = {
		id = "rbxasset://sounds/action_footsteps_plastic.mp3", vol = 0.20, pitch = 0.48,
		layers = {
			{ id = "rbxasset://sounds/action_footsteps_plastic.mp3", vol = 0.20, pitch = 0.48, role = "foley", pitchJitter = 0.10, randomVolume = 0.16, eq = { low = 2, mid = -2, high = -10 } },
			{ id = "rbxasset://sounds/collide.wav", vol = 0.08, pitch = 0.33, delay = 0.018, role = "foley", pitchJitter = 0.12, eq = { low = 5, mid = -5, high = -16 } },
		},
	},
	STEP_GRAVEL = {
		id = "rbxasset://sounds/action_footsteps_plastic.mp3", vol = 0.22, pitch = 0.58,
		layers = {
			{ id = "rbxasset://sounds/action_footsteps_plastic.mp3", vol = 0.20, pitch = 0.58, role = "foley", pitchJitter = 0.12, randomVolume = 0.18, eq = { low = -1, mid = 1, high = -5 } },
			{ id = "rbxasset://sounds/clickfast.wav", vol = 0.09, pitch = 0.52, delay = 0.012, role = "foley", pitchJitter = 0.16, eq = { low = -2, mid = 2, high = -4 } },
		},
	},
	GEAR_RATTLE = {
		id = "rbxasset://sounds/switch.wav",
		vol = 0.10,
		pitch = 1.12,
		layers = {
			{
				id = "rbxasset://sounds/switch.wav", vol = 0.075, pitch = 1.12,
				role = "foley", pitchJitter = 0.10, randomVolume = 0.20,
				eq = { low = -8, mid = 2, high = -1 },
			},
			{
				id = "rbxasset://sounds/clickfast.wav", vol = 0.050, pitch = 0.72, delay = 0.016,
				role = "foley", pitchJitter = 0.08,
				eq = { low = -3, mid = 2, high = -5 },
			},
		},
	},

	-- Weapon handling is quiet and close. Contacts are split into latch, polymer
	-- body and metal stop so the animation has readable mechanical landmarks.
	DRY_FIRE = {
		id = "rbxasset://sounds/clickfast.wav", vol = 0.34, pitch = 0.96,
		role = "mechanism", eq = { low = -8, mid = 4, high = 1 },
	},
	MAG_OUT = {
		id = "rbxasset://sounds/switch.wav", vol = 0.30, pitch = 0.78,
		layers = {
			{
				id = "rbxasset://sounds/clickfast.wav", vol = 0.22, pitch = 1.08,
				role = "mechanism", eq = { low = -9, mid = 4, high = 1 },
			},
			{
				id = "rbxasset://sounds/switch.wav", vol = 0.30, pitch = 0.78, delay = 0.034,
				role = "mechanism", pitchJitter = 0.035,
				eq = { low = -2, mid = 2, high = -5 },
			},
			{
				id = "rbxasset://sounds/collide.wav", vol = 0.075, pitch = 0.82, delay = 0.052,
				role = "foley", eq = { low = 1, mid = -2, high = -8 },
			},
		},
	},
	MAG_IN = {
		id = "rbxasset://sounds/switch3.wav", vol = 0.36, pitch = 0.76,
		layers = {
			{
				id = "rbxasset://sounds/switch.wav", vol = 0.14, pitch = 0.92,
				role = "mechanism", eq = { low = -4, mid = 2, high = -4 },
			},
			{
				id = "rbxasset://sounds/switch3.wav", vol = 0.36, pitch = 0.76, delay = 0.015,
				role = "mechanism", pitchJitter = 0.025,
				eq = { low = 2, mid = 1, high = -6 },
			},
			{
				id = "rbxasset://sounds/collide.wav", vol = 0.12, pitch = 0.68, delay = 0.028,
				role = "foley", eq = { low = 3, mid = -3, high = -10 },
			},
		},
	},
	BOLT = {
		id = "rbxasset://sounds/snap.mp3", vol = 0.40, pitch = 0.86,
		layers = {
			{
				id = "rbxasset://sounds/unsheath.wav", vol = 0.19, pitch = 1.28,
				role = "mechanism", eq = { low = -6, mid = 2, high = -2 },
			},
			{
				id = "rbxasset://sounds/snap.mp3", vol = 0.40, pitch = 0.86, delay = 0.024,
				role = "mechanism", pitchJitter = 0.020,
				eq = { low = 0, mid = 3, high = -2 },
			},
			{
				id = "rbxasset://sounds/collide.wav", vol = 0.10, pitch = 0.56, delay = 0.028,
				role = "mechanism", eq = { low = 4, mid = -4, high = -12 },
			},
		},
	},
	SELECTOR = {
		id = "rbxasset://sounds/clickfast.wav", vol = 0.23, pitch = 1.48,
		role = "mechanism", eq = { low = -10, mid = 4, high = 2 },
	},
	SHELL_IN = {
		id = "rbxasset://sounds/clickfast.wav", vol = 0.31, pitch = 0.64,
		layers = {
			{
				id = "rbxasset://sounds/switch3.wav", vol = 0.17, pitch = 0.50,
				role = "foley", eq = { low = 1, mid = -1, high = -7 },
			},
			{
				id = "rbxasset://sounds/clickfast.wav", vol = 0.31, pitch = 0.64, delay = 0.020,
				role = "mechanism", pitchJitter = 0.035,
				eq = { low = -2, mid = 3, high = -3 },
			},
		},
	},
	MAG_DROP = {
		id = "rbxasset://sounds/collide.wav", vol = 0.26, pitch = 0.58,
		layers = {
			{
				id = "rbxasset://sounds/collide.wav", vol = 0.26, pitch = 0.58,
				role = "foley", pitchJitter = 0.10, randomVolume = 0.15,
				eq = { low = 4, mid = -2, high = -8 },
			},
			{
				id = "rbxasset://sounds/switch.wav", vol = 0.12, pitch = 0.88, delay = 0.025,
				role = "foley", pitchJitter = 0.08,
				eq = { low = -5, mid = 2, high = -3 },
			},
		},
	},
	SWAP = {
		id = "rbxasset://sounds/unsheath.wav", vol = 0.28, pitch = 0.82,
		layers = {
			{
				id = "rbxasset://sounds/unsheath.wav", vol = 0.28, pitch = 0.82,
				role = "foley", pitchJitter = 0.04,
				eq = { low = -2, mid = 1, high = -4 },
			},
			{
				id = "rbxasset://sounds/switch.wav", vol = 0.09, pitch = 1.08, delay = 0.050,
				role = "mechanism", eq = { low = -7, mid = 2, high = -1 },
			},
		},
	},

	POUCH_DRAW = {
		id = "rbxasset://sounds/unsheath.wav", vol = 0.16, pitch = 0.74,
		layers = {
			{
				id = "rbxasset://sounds/unsheath.wav", vol = 0.16, pitch = 0.74,
				role = "foley", pitchJitter = 0.05,
				eq = { low = -2, mid = 1, high = -6 },
			},
			{
				id = "rbxasset://sounds/switch.wav", vol = 0.055, pitch = 0.90, delay = 0.022,
				role = "foley", eq = { low = -5, mid = 1, high = -5 },
			},
		},
	},
	MAG_CONTACT = {
		id = "rbxasset://sounds/collide.wav", vol = 0.16, pitch = 0.92,
		role = "mechanism", eq = { low = -1, mid = 2, high = -7 },
	},
	MAG_TAP = {
		id = "rbxasset://sounds/collide.wav", vol = 0.17, pitch = 0.70,
		role = "foley", eq = { low = 2, mid = -1, high = -8 },
	},
	BOLT_RELEASE = {
		id = "rbxasset://sounds/snap.mp3", vol = 0.34, pitch = 0.94,
		layers = {
			{
				id = "rbxasset://sounds/clickfast.wav", vol = 0.13, pitch = 1.20,
				role = "mechanism", eq = { low = -9, mid = 4, high = 1 },
			},
			{
				id = "rbxasset://sounds/snap.mp3", vol = 0.34, pitch = 0.94, delay = 0.018,
				role = "mechanism", eq = { low = 1, mid = 3, high = -3 },
			},
		},
	},
	CHARGING_HANDLE = {
		id = "rbxasset://sounds/unsheath.wav", vol = 0.24, pitch = 1.10,
		layers = {
			{
				id = "rbxasset://sounds/unsheath.wav", vol = 0.24, pitch = 1.10,
				role = "mechanism", eq = { low = -5, mid = 2, high = -2 },
			},
			{
				id = "rbxasset://sounds/snap.mp3", vol = 0.27, pitch = 0.88, delay = 0.085,
				role = "mechanism", eq = { low = 0, mid = 3, high = -3 },
			},
		},
	},

	-- Near-miss audio is separate from the muzzle report. A supersonic crack is
	-- bright and instantaneous; the pressure wake follows a few milliseconds
	-- later. This is what makes incoming fire read as a projectile passing the
	-- listener rather than a generic camera-shake cue.
	BULLET_CRACK = {
		id = "rbxasset://sounds/snap.mp3", vol = 0.54, pitch = 2.05,
		layers = {
			{ id = "rbxasset://sounds/snap.mp3", vol = 0.54, pitch = 2.05, role = "transient", pitchJitter = 0.035, eq = { low = -12, mid = 1, high = 6 } },
			{ id = "rbxasset://sounds/swoosh.wav", vol = 0.16, pitch = 1.62, delay = 0.012, role = "body", pitchJitter = 0.05, eq = { low = -9, mid = 1, high = 2 } },
		},
	},
	BULLET_WHIZZ = { id = "rbxasset://sounds/swoosh.wav", vol = 0.18, pitch = 1.36, role = "body", eq = { low = -10, mid = 2, high = 1 } },

	-- Material impacts are intentionally short. Long explosion-like impact tails
	-- make every wall strike sound like a grenade and destroy scale.
	IMPACT_CONCRETE = {
		id = "rbxasset://sounds/collide.wav", vol = 0.42, pitch = 0.82,
		layers = {
			{ id = "rbxasset://sounds/collide.wav", vol = 0.42, pitch = 0.82, role = "transient", pitchJitter = 0.08, eq = { low = 3, mid = 1, high = -6 } },
			{ id = "rbxasset://sounds/pop_mid.wav", vol = 0.08, pitch = 0.70, delay = 0.012, role = "reflection", eq = { low = -2, mid = -2, high = -12 }, reverb = { decay = 0.20, wet = -17, dry = -6 } },
		},
	},
	IMPACT_METAL = {
		id = "rbxasset://sounds/switch3.wav", vol = 0.40, pitch = 1.18,
		layers = {
			{ id = "rbxasset://sounds/snap.mp3", vol = 0.20, pitch = 1.75, role = "transient", pitchJitter = 0.08, eq = { low = -10, mid = 2, high = 5 } },
			{ id = "rbxasset://sounds/switch3.wav", vol = 0.40, pitch = 1.18, delay = 0.004, role = "reflection", pitchJitter = 0.10, eq = { low = -5, mid = 3, high = 1 }, reverb = { decay = 0.34, wet = -13, dry = -5 } },
		},
	},
	IMPACT_WOOD = { id = "rbxasset://sounds/collide.wav", vol = 0.36, pitch = 0.48, role = "transient", pitchJitter = 0.10, eq = { low = 6, mid = -1, high = -12 } },
	IMPACT_GLASS = { id = "rbxasset://sounds/snap.mp3", vol = 0.34, pitch = 1.72, role = "transient", pitchJitter = 0.08, eq = { low = -12, mid = 0, high = 5 } },
	IMPACT_DIRT = { id = "rbxasset://sounds/action_footsteps_plastic.mp3", vol = 0.22, pitch = 0.46, role = "foley", pitchJitter = 0.12, eq = { low = 3, mid = -3, high = -12 } },
	IMPACT_HARD = { id = "rbxasset://sounds/collide.wav", vol = 0.40, pitch = 0.90, role = "transient", pitchJitter = 0.08 },
	FLESH = {
		id = "rbxasset://sounds/impact_water.mp3", vol = 0.36, pitch = 0.78,
		layers = {
			{ id = "rbxasset://sounds/impact_water.mp3", vol = 0.30, pitch = 0.78, role = "body", pitchJitter = 0.08, eq = { low = 4, mid = -2, high = -11 } },
			{ id = "rbxasset://sounds/collide.wav", vol = 0.10, pitch = 0.42, delay = 0.006, role = "body", pitchJitter = 0.06, eq = { low = 5, mid = -5, high = -16 } },
		},
	},

	HEARTBEAT = { id = "rbxasset://sounds/bass.wav", vol = 0.62, pitch = 0.35 },
	BREATH = { id = "rbxasset://sounds/swoosh.wav", vol = 0.24, pitch = 0.45 },
	HURT = { id = "rbxasset://sounds/uuhhh.wav", vol = 0.50, pitch = 0.9 },

	ENTITY_SCREAM = {
		id = "rbxasset://sounds/uuhhh.wav", vol = 0.76, pitch = 0.28,
		layers = {
			{ id = "rbxasset://sounds/uuhhh.wav", vol = 0.76, pitch = 0.28, role = "body", pitchJitter = 0.025, eq = { low = 5, mid = 0, high = -9 }, compress = { attack = 0.018, threshold = -15, ratio = 4.5, release = 0.28 } },
			{ id = "rbxasset://sounds/uuhhh.wav", vol = 0.18, pitch = 0.35, role = "tail", eq = { low = 2, mid = -4, high = -14 }, minDistance = 18, maxDistance = 300 },
		},
	},
	ENTITY_BREATH = { id = "rbxasset://sounds/swoosh.wav", vol = 0.52, pitch = 0.28 },
	ENTITY_STEP = {
		id = "rbxasset://sounds/collide.wav", vol = 0.42, pitch = 0.34,
		layers = {
			{ id = "rbxasset://sounds/collide.wav", vol = 0.42, pitch = 0.34, role = "body", pitchJitter = 0.08, eq = { low = 7, mid = -3, high = -13 } },
			{ id = "rbxasset://sounds/action_footsteps_plastic.mp3", vol = 0.18, pitch = 0.46, delay = 0.018, role = "reflection", pitchJitter = 0.08, eq = { low = 0, mid = -1, high = -8 }, reverb = { decay = 0.28, wet = -14, dry = -7 } },
		},
	},

	POWER_ON = {
		id = "rbxasset://sounds/electronicpingshort.wav", vol = 0.58, pitch = 0.35,
		layers = {
			{ id = "rbxasset://sounds/electronicpingshort.wav", vol = 0.58, pitch = 0.35, role = "body", eq = { low = 3, mid = 0, high = -6 } },
			{ id = "rbxasset://sounds/bass.wav", vol = 0.20, pitch = 0.28, delay = 0.05, role = "tail", eq = { low = 5, mid = -6, high = -18 }, minDistance = 20, maxDistance = 600 },
		},
	},
	PICKUP = { id = "rbxasset://sounds/switch3.wav", vol = 0.24, pitch = 0.78, role = "mechanism", eq = { low = -2, mid = 2, high = -5 } },
	DOOR = {
		id = "rbxasset://sounds/switch.wav", vol = 0.42, pitch = 0.55,
		layers = {
			{ id = "rbxasset://sounds/switch.wav", vol = 0.34, pitch = 0.55, role = "mechanism", pitchJitter = 0.04, eq = { low = 2, mid = 1, high = -7 } },
			{ id = "rbxasset://sounds/collide.wav", vol = 0.24, pitch = 0.42, delay = 0.06, role = "body", pitchJitter = 0.05, eq = { low = 6, mid = -3, high = -13 } },
			{ id = "rbxasset://sounds/pop_mid.wav", vol = 0.07, pitch = 0.40, role = "reflection", eq = { low = 0, mid = -5, high = -16 }, reverb = { decay = 0.34, wet = -15, dry = -8 } },
		},
	},
	EXTRACT = { id = "rbxasset://sounds/bass.wav", vol = 0.38, pitch = 0.46, role = "tail", eq = { low = 5, mid = -5, high = -15 } },
}


--------------------------------------------------------------------------------
-- V15 RECORDED WEAPON REPORTS
--
-- Use one real close report per shot. The previous mixer doubled full gunshot
-- samples as "body" and "reflection", which produced comb-filtered, gamey
-- blasts. Environment is now handled by dynamic reverb/EQ on the same report;
-- the only second layer is quiet weapon mechanism.
--------------------------------------------------------------------------------
C.SFX.SHOT_RIFLE = {
	environmentAware = true,
	id = "rbxassetid://9113176721",
	vol = 0.92,
	layers = {
		{ variants = { "rbxassetid://9113176721", "rbxassetid://9113189096" },
			id = "rbxassetid://9113176721", vol = 0.92, role = "body", cut = 0.58,
			pitchJitter = 0.004, randomVolume = 0.018,
			eq = { low = 0.8, mid = -0.2, high = 0.4 },
			reverb = { dry = -1.0 },
			compress = { attack = 0.002, threshold = -8, ratio = 2.6, release = 0.12, makeup = 0.0 } },
		{ id = "rbxassetid://18806295412", vol = 0.105, role = "mechanism", delay = 0.024, cut = 0.26,
			pitch = 0.86, pitchJitter = 0.010, eq = { low = -7, mid = 1.2, high = -2.5 } },
	},
}
C.SFX.SHOT_PISTOL = {
	environmentAware = true,
	id = "rbxassetid://9114694945",
	vol = 0.86,
	layers = {
		{ id = "rbxassetid://9114694945", vol = 0.86, role = "body", cut = 0.48,
			pitchJitter = 0.005, randomVolume = 0.022,
			eq = { low = -0.5, mid = 0.2, high = 0.8 }, reverb = { dry = -1.0 },
			compress = { attack = 0.002, threshold = -9, ratio = 2.5, release = 0.10 } },
		{ id = "rbxassetid://18806295412", vol = 0.14, role = "mechanism", delay = 0.018, cut = 0.24,
			pitch = 1.08, pitchJitter = 0.012, eq = { low = -8, mid = 1.8, high = -1.5 } },
	},
}
C.SFX.SHOT_SHOTGUN = {
	environmentAware = true,
	id = "rbxassetid://9112912106",
	vol = 0.98,
	layers = {
		{ id = "rbxassetid://9112912106", vol = 0.98, role = "body", cut = 0.72,
			pitchJitter = 0.003, randomVolume = 0.015,
			eq = { low = 1.8, mid = -0.4, high = -1.0 }, reverb = { dry = -1.0 },
			compress = { attack = 0.003, threshold = -7, ratio = 2.8, release = 0.18 } },
	},
}
C.SFX.SHOT_SUPPRESSED = {
	environmentAware = true,
	id = "rbxassetid://9119136387",
	vol = 0.52,
	layers = {
		{ variants = { "rbxassetid://9119136387", "rbxassetid://9119134226" },
			id = "rbxassetid://9119136387", vol = 0.52, role = "body", cut = 0.42,
			pitchJitter = 0.004, randomVolume = 0.02,
			eq = { low = -2.5, mid = -0.6, high = -3.5 }, reverb = { dry = -1.0 },
			compress = { attack = 0.003, threshold = -13, ratio = 2.2, release = 0.10 } },
	},
}

-- Handling foley: recorded cloth/weapon handling replaces cartoon switch/pop
-- placeholders for the actions the player hears constantly.
C.SFX.GEAR = { id = "rbxassetid://9113823112", vol = 0.12, role = "foley", cut = 0.42,
	pitchJitter = 0.035, eq = { low = -4, mid = 0, high = -5 } }
C.SFX.MAG_OUT = { id = "rbxassetid://9114702589", vol = 0.17, role = "mechanism", cut = 0.30,
	startAt = 0.04, pitch = 1.12, pitchJitter = 0.018, eq = { low = -5, mid = 1, high = -4 } }
C.SFX.MAG_IN = { id = "rbxassetid://8145744063", vol = 0.24, role = "mechanism", cut = 0.40,
	pitch = 0.92, pitchJitter = 0.012, eq = { low = -2, mid = 1.5, high = -3 } }
C.SFX.MAG_CONTACT = { id = "rbxassetid://9114702589", vol = 0.13, role = "mechanism", startAt = 0.22, cut = 0.24,
	pitch = 1.28, pitchJitter = 0.014, eq = { low = -8, mid = 2, high = -4 } }
C.SFX.BOLT = { id = "rbxassetid://18806295412", vol = 0.23, role = "mechanism", cut = 0.38,
	pitch = 0.88, pitchJitter = 0.010, eq = { low = -3, mid = 1, high = -2 } }
C.SFX.CHARGE = { id = "rbxassetid://9114702589", vol = 0.17, role = "mechanism", startAt = 0.34, cut = 0.44,
	pitch = 0.82, pitchJitter = 0.012, eq = { low = -4, mid = 0.8, high = -4 } }
C.SFX.MAG_DROP = { id = "rbxassetid://9113109574", vol = 0.20, role = "world", cut = 1.0,
	pitchJitter = 0.025, eq = { low = -2, mid = 0, high = -4 } }

--------------------------------------------------------------------------------
-- ANTI-CHEAT-ISH SERVER LIMITS
-- This is a PvE game so the client owns weapon feel. The server still refuses
-- shots that are physically impossible.
--------------------------------------------------------------------------------
C.LIMITS = {
	MAX_SHOTS_PER_SEC = 22,
	MAX_ORIGIN_ERROR = 14, -- studs the claimed muzzle may be from the head
	MAX_HIT_ERROR = 8, -- studs the claimed hit may be from the ray
}

return C
