--!nonstrict
-- Weapons -- data-only definitions. No behaviour lives here.
--
-- GEOMETRY CONVENTION for `model`:
--   The gun's local origin is the firing hand's grip. The weapon points down
--   its own -Z (Roblox LookVector), +Y is up, +X is right. Every offset is in
--   studs from that grip. `sightOffset` is the optical centre of the sight in
--   the same space -- the client solves the aim pose by moving that point onto
--   the camera axis, so if you move an optic in `model` you must move
--   `sightOffset` with it or the sights will not line up.

local Cfg = require(script.Parent:WaitForChild("GameConfig"))

export type ModelPart = {
	n: string,
	size: Vector3,
	pos: Vector3,
	rot: Vector3?, -- degrees, XYZ
	color: Color3,
	mat: Enum.Material?,
	optional: string?, -- only built when this feature is on ("suppressor")

	-- THE SCOPE BUG. Material = Glass does NOT make a part see-through in
	-- Roblox -- Transparency does, and Transparency defaults to 0. Both optic
	-- lenses were fully opaque green blocks sitting across the sight line, which
	-- is why aiming still pointed you at the back of a brick even after the
	-- housing was rebuilt hollow. The rear lens especially has to be almost
	-- fully transparent: you are looking THROUGH it, not at it.
	transparency: number?,

	-- "cylinder" gives a real round barrel instead of a square one. Authored in
	-- the same Z-forward convention as everything else; the builder handles
	-- Roblox's Cylinder having its axis on X.
	shape: string?,

	-- Animatable sub-assembly. Parts tagged with a bone get the animation
	-- system's offset for that bone applied on top of their static position,
	-- which is what lets the slide reciprocate and the magazine fall out.
	-- Known bones: "mag", "bolt", "pump", "charge".
	bone: string?,
}

-- Where the hands go, in weapon-local space. The animation system drives the
-- hands to these anchors, and two-bone IK builds the arms to reach them, so
-- moving a handguard here moves the support hand with it for free.
export type Grips = {
	right: CFrame, -- firing hand on the pistol grip
	left: CFrame, -- support hand at rest on the handguard / pump
	magwell: CFrame, -- support hand stripping or seating a magazine
	pouch: CFrame, -- down at the rig, mostly out of frame
	charge: CFrame, -- charging handle / slide serrations
	release: CFrame, -- bolt release, or the shotgun's loading port
}

export type Weapon = {
	id: string,
	name: string,
	class: string, -- "primary" | "sidearm"
	feed: string, -- "mag" | "tube"

	damage: number,
	pellets: number,
	rpm: number,
	fireModes: { string }, -- cycled by the selector, always starts on SAFE
	burstCount: number,

	magCap: number,
	magsCarried: number,

	-- Cone half-angles in radians.
	spreadHip: number,
	spreadAds: number,
	spreadMove: number, -- added, scaled by how fast you are moving
	spreadBloom: number, -- added per shot
	bloomDecay: number, -- radians shed per second

	recoilPitch: number, -- radians of muzzle rise per shot
	recoilYaw: number, -- random, +/-
	recoilKick: number, -- studs the gun punches back into the shoulder
	recoilRecover: number,

	adsFov: number,
	adsTime: number, -- seconds to present the sights. A property of the gun.
	weight: number, -- kg, loaded. Drives handling, not damage.
	sightOffset: Vector3,
	ergonomics: number, -- 0..1, higher = less sway, faster handling

	suppressible: boolean,
	shotSfx: string,

	model: { ModelPart },
	grips: Grips,
	boltTravel: number, -- studs the bolt/slide runs back when it cycles

	-- Rotation pivots, in weapon-local space. Assigned below the definitions
	-- so they sit together and can be read as a group. See the PIVOTS block.
	pocket: Vector3?, -- butt pad in the shoulder: recoil turns about this
	wrist: Vector3?, -- firing hand: sway and inertia turn about this
	reach: number?, -- studs root to muzzle, for weapon collision
}

local function deg(x: number, y: number, z: number): CFrame
	return CFrame.Angles(math.rad(x), math.rad(y), math.rad(z))
end

local BLACK = Color3.fromRGB(53, 57, 64)
local GUNMETAL = Color3.fromRGB(80, 85, 94)
local POLY = Color3.fromRGB(59, 64, 61)
local FDE = Color3.fromRGB(112, 100, 78)
local GLASS = Color3.fromRGB(60, 90, 78)

local M = Enum.Material

local Weapons: { [string]: Weapon } = {}

--------------------------------------------------------------------------------
-- MK18 -- 5.56x45 short-barrelled carbine. The workhorse.
--------------------------------------------------------------------------------
Weapons.MK18 = {
	id = "MK18",
	name = "MK18 MOD 1",
	class = "primary",
	feed = "mag",

	damage = 34,
	pellets = 1,
	rpm = 780,
	fireModes = { "SAFE", "SEMI", "AUTO" },
	burstCount = 3,

	magCap = 30,
	magsCarried = 6,

	spreadHip = math.rad(3.4),
	spreadAds = math.rad(0.22),
	spreadMove = math.rad(2.2),
	spreadBloom = math.rad(0.30),
	bloomDecay = math.rad(2.6),

	recoilPitch = math.rad(0.95),
	recoilYaw = math.rad(0.34),
	recoilKick = 0.115,
	recoilRecover = 11.0,

	adsFov = 46,
	adsTime = 0.42, -- 3.1 kg carbine. Deliberate. You feel it come up.
	weight = 3.1,
	-- IRON SIGHTS. Measured off the converted mesh: the source's rear_sight sits
	-- at z 0.047 and y -0.126, which after the 4.758 studs/m scale and the grip
	-- offset puts the aperture at (0, 0.626, -0.02). Using the REAR aperture as
	-- the point that goes on the optical axis is what leaves the front post out
	-- at z -1.83 where you can see it -- i.e. an actual sight picture with two
	-- elements, rather than a single floating dot.
	sightOffset = Vector3.new(0, 0.449, -0.015),
	ergonomics = 0.72,

	suppressible = true,
	shotSfx = "SHOT_RIFLE",
	boltTravel = 0.20,

	model = {
		-- Lower receiver group
		{ n = "LowerReceiver", size = Vector3.new(0.26, 0.30, 0.92), pos = Vector3.new(0, 0.20, -0.20), color = GUNMETAL, mat = M.Metal },
		{ n = "Magwell", size = Vector3.new(0.24, 0.30, 0.34), pos = Vector3.new(0, 0.10, -0.34), rot = Vector3.new(4, 0, 0), color = GUNMETAL, mat = M.Metal },
		{ n = "MagwellFlare", size = Vector3.new(0.27, 0.09, 0.38), pos = Vector3.new(0, -0.05, -0.35), rot = Vector3.new(5, 0, 0), color = BLACK, mat = M.Metal },
		{ n = "TriggerGuard", size = Vector3.new(0.13, 0.05, 0.34), pos = Vector3.new(0, -0.05, -0.02), color = GUNMETAL, mat = M.Metal },
		{ n = "TriggerGuardRear", size = Vector3.new(0.13, 0.16, 0.05), pos = Vector3.new(0, 0.02, 0.13), color = GUNMETAL, mat = M.Metal },
		{ n = "Trigger", size = Vector3.new(0.05, 0.13, 0.06), pos = Vector3.new(0, 0.05, -0.02), rot = Vector3.new(-8, 0, 0), color = BLACK, mat = M.Metal },
		{ n = "Selector", size = Vector3.new(0.30, 0.07, 0.09), pos = Vector3.new(0, 0.16, 0.08), color = BLACK, mat = M.Metal },
		{ n = "MagRelease", size = Vector3.new(0.31, 0.07, 0.07), pos = Vector3.new(0, 0.14, -0.16), color = BLACK, mat = M.Metal },
		{ n = "BoltCatch", size = Vector3.new(0.30, 0.06, 0.14), pos = Vector3.new(-0.01, 0.20, -0.05), color = BLACK, mat = M.Metal },

		-- Upper receiver group
		{ n = "UpperReceiver", size = Vector3.new(0.25, 0.24, 1.02), pos = Vector3.new(0, 0.41, -0.28), color = GUNMETAL, mat = M.Metal },
		{ n = "UpperRail", size = Vector3.new(0.19, 0.06, 1.02), pos = Vector3.new(0, 0.54, -0.28), color = BLACK, mat = M.Metal },
		{ n = "EjectionPort", size = Vector3.new(0.27, 0.15, 0.30), pos = Vector3.new(0.01, 0.38, -0.12), color = Color3.fromRGB(20, 21, 22), mat = M.Metal },
		{ n = "ForwardAssist", size = Vector3.new(0.13, 0.11, 0.11), pos = Vector3.new(0.14, 0.46, 0.14), color = BLACK, mat = M.Metal },
		{ n = "BrassDeflector", size = Vector3.new(0.10, 0.13, 0.16), pos = Vector3.new(0.15, 0.44, -0.02), color = GUNMETAL, mat = M.Metal },

		-- Handguard: M-LOK rail sections built as separate slats so the
		-- silhouette breaks up instead of reading as one extruded block.
		{ n = "HandguardTop", size = Vector3.new(0.21, 0.07, 1.00), pos = Vector3.new(0, 0.53, -1.24), color = BLACK, mat = M.Metal },
		{ n = "HandguardBody", size = Vector3.new(0.23, 0.24, 1.00), pos = Vector3.new(0, 0.38, -1.24), color = POLY, mat = M.Metal },
		{ n = "HandguardBottom", size = Vector3.new(0.19, 0.06, 0.98), pos = Vector3.new(0, 0.24, -1.24), color = BLACK, mat = M.Metal },
		{ n = "MlokL1", size = Vector3.new(0.03, 0.14, 0.16), pos = Vector3.new(-0.12, 0.38, -0.98), color = BLACK, mat = M.Metal },
		{ n = "MlokL2", size = Vector3.new(0.03, 0.14, 0.16), pos = Vector3.new(-0.12, 0.38, -1.24), color = BLACK, mat = M.Metal },
		{ n = "MlokL3", size = Vector3.new(0.03, 0.14, 0.16), pos = Vector3.new(-0.12, 0.38, -1.50), color = BLACK, mat = M.Metal },
		{ n = "MlokR1", size = Vector3.new(0.03, 0.14, 0.16), pos = Vector3.new(0.12, 0.38, -0.98), color = BLACK, mat = M.Metal },
		{ n = "MlokR2", size = Vector3.new(0.03, 0.14, 0.16), pos = Vector3.new(0.12, 0.38, -1.24), color = BLACK, mat = M.Metal },
		{ n = "MlokR3", size = Vector3.new(0.03, 0.14, 0.16), pos = Vector3.new(0.12, 0.38, -1.50), color = BLACK, mat = M.Metal },
		{ n = "BarrelNut", size = Vector3.new(0.20, 0.20, 0.14), pos = Vector3.new(0, 0.38, -0.76), color = GUNMETAL, mat = M.Metal, shape = "cylinder" },

		-- Barrel group. Round things are round now -- a square barrel is the
		-- single most obvious "this is boxes" tell on the whole weapon.
		{ n = "GasBlock", size = Vector3.new(0.15, 0.17, 0.15), pos = Vector3.new(0, 0.44, -1.74), color = BLACK, mat = M.Metal },
		{ n = "GasTube", size = Vector3.new(0.05, 0.05, 0.85), pos = Vector3.new(0, 0.50, -1.32), color = GUNMETAL, mat = M.Metal, shape = "cylinder" },
		{ n = "Barrel", size = Vector3.new(0.10, 0.10, 0.42), pos = Vector3.new(0, 0.38, -1.96), color = BLACK, mat = M.Metal, shape = "cylinder" },
		{ n = "MuzzleDevice", size = Vector3.new(0.13, 0.13, 0.24), pos = Vector3.new(0, 0.38, -2.28), color = BLACK, mat = M.Metal, shape = "cylinder" },
		{ n = "BrakePort1", size = Vector3.new(0.15, 0.04, 0.04), pos = Vector3.new(0, 0.44, -2.24), color = Color3.fromRGB(14, 14, 15), mat = M.Metal },
		{ n = "BrakePort2", size = Vector3.new(0.15, 0.04, 0.04), pos = Vector3.new(0, 0.44, -2.32), color = Color3.fromRGB(14, 14, 15), mat = M.Metal },

		-- Stock group
		{ n = "BufferTube", size = Vector3.new(0.17, 0.17, 0.66), pos = Vector3.new(0, 0.40, 0.52), color = GUNMETAL, mat = M.Metal, shape = "cylinder" },
		{ n = "CastleNut", size = Vector3.new(0.21, 0.21, 0.09), pos = Vector3.new(0, 0.40, 0.24), color = BLACK, mat = M.Metal, shape = "cylinder" },
		{ n = "StockBody", size = Vector3.new(0.22, 0.30, 0.40), pos = Vector3.new(0, 0.34, 0.66), color = POLY, mat = M.Metal },
		{ n = "StockToe", size = Vector3.new(0.20, 0.20, 0.16), pos = Vector3.new(0, 0.20, 0.80), color = POLY, mat = M.Metal },
		{ n = "ButtPad", size = Vector3.new(0.23, 0.42, 0.08), pos = Vector3.new(0, 0.31, 0.90), color = Color3.fromRGB(20, 20, 21), mat = M.Fabric },
		{ n = "CheekRest", size = Vector3.new(0.19, 0.07, 0.34), pos = Vector3.new(0, 0.50, 0.62), color = POLY, mat = M.Fabric },
		{ n = "SlingMount", size = Vector3.new(0.26, 0.08, 0.08), pos = Vector3.new(0, 0.30, 0.26), color = BLACK, mat = M.Metal },

		-- Pistol grip
		{ n = "Grip", size = Vector3.new(0.16, 0.42, 0.20), pos = Vector3.new(0, -0.10, 0.14), rot = Vector3.new(-17, 0, 0), color = POLY, mat = M.Metal },
		{ n = "GripBase", size = Vector3.new(0.17, 0.06, 0.21), pos = Vector3.new(0, -0.30, 0.20), rot = Vector3.new(-17, 0, 0), color = BLACK, mat = M.Metal },

		-- Furniture
		{ n = "Foregrip", size = Vector3.new(0.13, 0.32, 0.13), pos = Vector3.new(0, 0.08, -1.36), rot = Vector3.new(-12, 0, 0), color = BLACK, mat = M.Metal },
		{ n = "LightBody", size = Vector3.new(0.13, 0.13, 0.36), pos = Vector3.new(-0.17, 0.30, -1.46), color = BLACK, mat = M.Metal, shape = "cylinder" },
		{ n = "LightLens", size = Vector3.new(0.10, 0.10, 0.03), pos = Vector3.new(-0.17, 0.30, -1.65), color = Color3.fromRGB(180, 180, 165), mat = M.Glass, transparency = 0.4, shape = "cylinder" },

		-- Optic. A red dot is a HOLLOW TUBE -- the previous version was a solid
		-- box across the sight line, so aiming pointed you at the back of a
		-- brick. This is built as four thin walls around an open bore with a
		-- glass pane at each end, so you can actually see the target through
		-- it, plus an emissive dot floating on the sight line.
		--
		-- It sits a realistic ~7 cm over the bore. Mount it higher and the ADS
		-- pose drops the whole gun far enough that the firing arm crumples
		-- into the shoulder -- if you raise it, re-check ARMS reach.
		{ n = "OpticMount", size = Vector3.new(0.17, 0.10, 0.42), pos = Vector3.new(0, 0.50, -0.38), color = BLACK, mat = M.Metal },
		{ n = "OpticTop", size = Vector3.new(0.17, 0.030, 0.40), pos = Vector3.new(0, 0.665, -0.40), color = BLACK, mat = M.Metal },
		{ n = "OpticBottom", size = Vector3.new(0.17, 0.030, 0.40), pos = Vector3.new(0, 0.560, -0.40), color = BLACK, mat = M.Metal },
		{ n = "OpticLeft", size = Vector3.new(0.030, 0.135, 0.40), pos = Vector3.new(-0.070, 0.612, -0.40), color = BLACK, mat = M.Metal },
		{ n = "OpticRight", size = Vector3.new(0.030, 0.135, 0.40), pos = Vector3.new(0.070, 0.612, -0.40), color = BLACK, mat = M.Metal },
		-- Transparency, not just Material. 0.86 on the objective still reads as
		-- coated glass; the ocular is 0.95 because your eye is behind it.
		{ n = "OpticGlassFront", size = Vector3.new(0.115, 0.105, 0.012), pos = Vector3.new(0, 0.612, -0.595), color = GLASS, mat = M.Glass, transparency = 0.86 },
		{ n = "OpticGlassRear", size = Vector3.new(0.115, 0.105, 0.012), pos = Vector3.new(0, 0.612, -0.205), color = GLASS, mat = M.Glass, transparency = 0.95 },
		{ n = "OpticDot", size = Vector3.new(0.020, 0.020, 0.020), pos = Vector3.new(0, 0.612, -0.560), color = Color3.fromRGB(255, 44, 30), mat = M.Neon, shape = "ball" },
		{ n = "OpticDial", size = Vector3.new(0.10, 0.09, 0.09), pos = Vector3.new(0.11, 0.612, -0.24), color = BLACK, mat = M.Metal },
		{ n = "FrontSight", size = Vector3.new(0.05, 0.09, 0.04), pos = Vector3.new(0, 0.56, -1.68), color = BLACK, mat = M.Metal },

		-- Moving parts
		{ n = "Magazine", size = Vector3.new(0.16, 0.62, 0.26), pos = Vector3.new(0, -0.20, -0.34), rot = Vector3.new(5, 0, 0), color = POLY, mat = M.Metal, bone = "mag" },
		{ n = "MagFloorplate", size = Vector3.new(0.18, 0.05, 0.28), pos = Vector3.new(0, -0.51, -0.37), rot = Vector3.new(5, 0, 0), color = BLACK, mat = M.Metal, bone = "mag" },
		{ n = "Bolt", size = Vector3.new(0.24, 0.12, 0.26), pos = Vector3.new(0.01, 0.38, -0.06), color = Color3.fromRGB(104, 100, 92), mat = M.Metal, bone = "bolt" },
		{ n = "ChargingHandle", size = Vector3.new(0.32, 0.08, 0.14), pos = Vector3.new(0, 0.53, 0.24), color = BLACK, mat = M.Metal, bone = "charge" },
		{ n = "ChargingLatch", size = Vector3.new(0.11, 0.07, 0.10), pos = Vector3.new(-0.14, 0.53, 0.22), color = BLACK, mat = M.Metal, bone = "charge" },

		{ n = "Suppressor", size = Vector3.new(0.17, 0.17, 0.74), pos = Vector3.new(0, 0.38, -2.50), color = Color3.fromRGB(34, 33, 32), mat = M.Metal, optional = "suppressor", shape = "cylinder" },
		{ n = "SuppressorRing", size = Vector3.new(0.19, 0.19, 0.06), pos = Vector3.new(0, 0.38, -2.18), color = BLACK, mat = M.Metal, optional = "suppressor", shape = "cylinder" },
	},

	-- Anchors track the rebuilt model: the bore sits at y 0.38 now, so the
	-- support hand is under the handguard at 0.16 rather than 0.08.
	-- Every offset below was multiplied by 0.7146 when the mesh scale dropped
	-- from 4.758 to 3.40 studs/m (the weapon was reading as the size of a car).
	-- tools/mk18_import.py prints that factor -- rescale these with it any time
	-- STUDS_PER_M changes, or the hands end up floating off the weapon.
	--
	-- `pouch` is the exception: it is a point on your CHEST RIG, not on the gun,
	-- so it must not shrink with the weapon. Scaled only enough to keep the arm
	-- reach sane.
	grips = {
		right = CFrame.new(0, -0.036, 0.093) * deg(-14, 0, 0),
		left = CFrame.new(0, 0.114, -0.943) * deg(-8, 0, 0),
		magwell = CFrame.new(0, -0.272, -0.257) * deg(4, 0, 0),
		pouch = CFrame.new(-0.30, -1.20, 0.16) * deg(-52, 22, 10),
		charge = CFrame.new(0.043, 0.400, 0.171) * deg(-4, 0, -14),
		release = CFrame.new(-0.171, 0.143, -0.036) * deg(0, 0, -22),
	},
}

--------------------------------------------------------------------------------
-- M870 -- 12 gauge pump. Tube fed, one shell at a time, manual cycle.
-- The horror gun: enormous close-range authority, punishing to run dry.
--------------------------------------------------------------------------------
Weapons.M870 = {
	id = "M870",
	name = "M870 BREACHER",
	class = "primary",
	feed = "tube",

	damage = 17, -- per pellet
	pellets = 9,
	rpm = 75, -- limited by the pump, not the trigger
	fireModes = { "SAFE", "SEMI" },
	burstCount = 1,

	magCap = 6,
	magsCarried = 24, -- loose shells, not magazines

	spreadHip = math.rad(5.6),
	spreadAds = math.rad(2.8),
	spreadMove = math.rad(1.8),
	spreadBloom = 0,
	bloomDecay = math.rad(4.0),

	recoilPitch = math.rad(4.6),
	recoilYaw = math.rad(0.9),
	recoilKick = 0.34,
	recoilRecover = 7.5,

	adsFov = 62,
	adsTime = 0.55, -- heaviest thing you carry, and it presents like it
	weight = 3.6,
	sightOffset = Vector3.new(0, 0.42, -0.40),
	ergonomics = 0.55,

	suppressible = false,
	shotSfx = "SHOT_SHOTGUN",
	boltTravel = 0.55, -- the pump, and you can see all of it

	model = {
		{ n = "Receiver", size = Vector3.new(0.26, 0.36, 1.05), pos = Vector3.new(0, 0.26, -0.25), color = BLACK, mat = M.Metal },
		{ n = "Barrel", size = Vector3.new(0.15, 0.15, 1.55), pos = Vector3.new(0, 0.34, -1.55), color = BLACK, mat = M.Metal, shape = "cylinder" },
		{ n = "Tube", size = Vector3.new(0.13, 0.13, 1.35), pos = Vector3.new(0, 0.14, -1.45), color = GUNMETAL, mat = M.Metal, shape = "cylinder" },
		{ n = "Pump", size = Vector3.new(0.20, 0.20, 0.55), pos = Vector3.new(0, 0.14, -1.30), color = POLY, mat = M.Metal, bone = "pump", shape = "cylinder" },
		{ n = "Stock", size = Vector3.new(0.22, 0.36, 0.95), pos = Vector3.new(0, 0.10, 0.72), rot = Vector3.new(4, 0, 0), color = FDE, mat = M.Metal },
		{ n = "Grip", size = Vector3.new(0.16, 0.40, 0.20), pos = Vector3.new(0, -0.08, 0.16), rot = Vector3.new(-14, 0, 0), color = FDE, mat = M.Metal },
		{ n = "Bead", size = Vector3.new(0.05, 0.06, 0.05), pos = Vector3.new(0, 0.44, -2.28), color = Color3.fromRGB(210, 180, 60), mat = M.Neon },
		{ n = "SideSaddle", size = Vector3.new(0.06, 0.20, 0.60), pos = Vector3.new(-0.16, 0.26, -0.20), color = POLY, mat = M.Metal },
		{ n = "Shell1", size = Vector3.new(0.04, 0.10, 0.10), pos = Vector3.new(-0.20, 0.32, -0.38), color = Color3.fromRGB(120, 30, 28), mat = M.Plastic },
		{ n = "Shell2", size = Vector3.new(0.04, 0.10, 0.10), pos = Vector3.new(-0.20, 0.32, -0.20), color = Color3.fromRGB(120, 30, 28), mat = M.Plastic },
		{ n = "Shell3", size = Vector3.new(0.04, 0.10, 0.10), pos = Vector3.new(-0.20, 0.32, -0.02), color = Color3.fromRGB(120, 30, 28), mat = M.Plastic },
		{ n = "PumpRib1", size = Vector3.new(0.22, 0.05, 0.05), pos = Vector3.new(0, 0.14, -1.44), color = BLACK, mat = M.Metal, bone = "pump" },
		{ n = "PumpRib2", size = Vector3.new(0.22, 0.05, 0.05), pos = Vector3.new(0, 0.14, -1.30), color = BLACK, mat = M.Metal, bone = "pump" },
		{ n = "PumpRib3", size = Vector3.new(0.22, 0.05, 0.05), pos = Vector3.new(0, 0.14, -1.16), color = BLACK, mat = M.Metal, bone = "pump" },
		{ n = "TriggerGuard", size = Vector3.new(0.13, 0.05, 0.32), pos = Vector3.new(0, -0.02, 0.00), color = BLACK, mat = M.Metal },
		{ n = "EjectionPort", size = Vector3.new(0.27, 0.13, 0.26), pos = Vector3.new(0.01, 0.26, -0.32), color = Color3.fromRGB(18, 19, 20), mat = M.Metal },
		{ n = "MagCap", size = Vector3.new(0.15, 0.15, 0.10), pos = Vector3.new(0, 0.14, -2.15), color = BLACK, mat = M.Metal },
	},

	grips = {
		right = CFrame.new(0, -0.03, 0.15) * deg(-11, 0, 0),
		-- The support hand rides the pump, so it travels with it automatically.
		left = CFrame.new(0, -0.02, -1.30) * deg(-6, 0, 0),
		magwell = CFrame.new(0, -0.10, -0.18) * deg(10, 0, 0), -- loading port
		pouch = CFrame.new(-0.36, -1.20, 0.22) * deg(-46, 26, 12),
		charge = CFrame.new(0, -0.02, -1.30) * deg(-6, 0, 0),
		release = CFrame.new(-0.20, 0.08, -0.10) * deg(0, 0, -18),
	},
}

--------------------------------------------------------------------------------
-- P320 -- 9x19 sidearm. What you have left when the carbine is empty.
--------------------------------------------------------------------------------
Weapons.P320 = {
	id = "P320",
	name = "P320 COMPACT",
	class = "sidearm",
	feed = "mag",

	damage = 22,
	pellets = 1,
	rpm = 430,
	fireModes = { "SAFE", "SEMI" },
	burstCount = 1,

	magCap = 17,
	magsCarried = 4,

	spreadHip = math.rad(4.6),
	spreadAds = math.rad(0.55),
	spreadMove = math.rad(2.6),
	spreadBloom = math.rad(0.55),
	bloomDecay = math.rad(3.4),

	recoilPitch = math.rad(1.55),
	recoilYaw = math.rad(0.55),
	recoilKick = 0.09,
	recoilRecover = 13.0,

	adsFov = 58,
	adsTime = 0.24, -- it is on target before the carbine has cleared your chest
	weight = 0.85,
	sightOffset = Vector3.new(0, 0.30, -0.20),
	ergonomics = 0.88,

	suppressible = true,
	shotSfx = "SHOT_PISTOL",
	boltTravel = 0.26,

	model = {
		-- The whole slide is one bone: on a pistol the reciprocating mass is
		-- right under your eye, so this is the most visible animation in the game.
		{ n = "Slide", size = Vector3.new(0.16, 0.22, 0.78), pos = Vector3.new(0, 0.26, -0.22), color = GUNMETAL, mat = M.Metal, bone = "bolt" },
		{ n = "FrontSight", size = Vector3.new(0.03, 0.07, 0.03), pos = Vector3.new(0, 0.40, -0.56), color = Color3.fromRGB(220, 220, 220), mat = M.Neon, bone = "bolt" },
		{ n = "RearSight", size = Vector3.new(0.14, 0.06, 0.05), pos = Vector3.new(0, 0.39, 0.10), color = BLACK, mat = M.Metal, bone = "bolt" },
		{ n = "Frame", size = Vector3.new(0.15, 0.14, 0.62), pos = Vector3.new(0, 0.09, -0.20), color = POLY, mat = M.Metal },
		{ n = "Grip", size = Vector3.new(0.15, 0.52, 0.22), pos = Vector3.new(0, -0.18, 0.06), rot = Vector3.new(-12, 0, 0), color = POLY, mat = M.Metal },
		{ n = "Magazine", size = Vector3.new(0.13, 0.50, 0.19), pos = Vector3.new(0, -0.19, 0.05), rot = Vector3.new(-12, 0, 0), color = GUNMETAL, mat = M.Metal, bone = "mag" },
		{ n = "Rail", size = Vector3.new(0.10, 0.06, 0.26), pos = Vector3.new(0, 0.01, -0.42), color = BLACK, mat = M.Metal },
		{ n = "Serration1", size = Vector3.new(0.17, 0.14, 0.03), pos = Vector3.new(0, 0.28, 0.08), color = BLACK, mat = M.Metal, bone = "bolt" },
		{ n = "Serration2", size = Vector3.new(0.17, 0.14, 0.03), pos = Vector3.new(0, 0.28, 0.02), color = BLACK, mat = M.Metal, bone = "bolt" },
		{ n = "Serration3", size = Vector3.new(0.17, 0.14, 0.03), pos = Vector3.new(0, 0.28, -0.04), color = BLACK, mat = M.Metal, bone = "bolt" },
		{ n = "EjectionPort", size = Vector3.new(0.17, 0.10, 0.20), pos = Vector3.new(0.01, 0.31, -0.12), color = Color3.fromRGB(18, 19, 20), mat = M.Metal, bone = "bolt" },
		{ n = "TriggerGuard", size = Vector3.new(0.11, 0.04, 0.24), pos = Vector3.new(0, -0.04, -0.10), color = POLY, mat = M.Metal },
		{ n = "Trigger", size = Vector3.new(0.04, 0.11, 0.05), pos = Vector3.new(0, 0.03, -0.10), rot = Vector3.new(-8, 0, 0), color = BLACK, mat = M.Metal },
		{ n = "MagBase", size = Vector3.new(0.16, 0.04, 0.21), pos = Vector3.new(0, -0.44, 0.12), rot = Vector3.new(-12, 0, 0), color = BLACK, mat = M.Metal, bone = "mag" },
		{ n = "Beavertail", size = Vector3.new(0.15, 0.07, 0.14), pos = Vector3.new(0, 0.11, 0.14), rot = Vector3.new(-30, 0, 0), color = POLY, mat = M.Metal },
		{ n = "Suppressor", size = Vector3.new(0.15, 0.15, 0.60), pos = Vector3.new(0, 0.26, -0.90), color = BLACK, mat = M.Metal, optional = "suppressor" },
	},

	grips = {
		right = CFrame.new(0, -0.12, 0.06) * deg(-10, 0, 0),
		-- Thumbs-forward support hand wrapped over the firing hand.
		left = CFrame.new(-0.17, -0.06, -0.10) * deg(-6, 0, 26),
		magwell = CFrame.new(0, -0.52, 0.12) * deg(-12, 0, 0),
		pouch = CFrame.new(-0.32, -1.28, 0.16) * deg(-48, 24, 10),
		charge = CFrame.new(-0.10, 0.30, 0.06) * deg(0, 0, 34),
		release = CFrame.new(-0.16, 0.06, 0.02) * deg(0, 0, -20),
	},
}

--------------------------------------------------------------------------------
-- Helpers
--------------------------------------------------------------------------------

-- Seconds between rounds.
function Weapons.cycleTime(w: Weapon): number
	return 60 / w.rpm
end

-- Where the muzzle sits in gun-local space, suppressor included.
function Weapons.muzzleOffset(w: Weapon, suppressed: boolean): Vector3
	local best: Vector3? = nil
	for _, p in w.model do
		if p.optional == "suppressor" and not suppressed then
			continue
		end
		local tip = p.pos - Vector3.new(0, 0, p.size.Z * 0.5)
		if best == nil or tip.Z < best.Z then
			best = tip
		end
	end
	return best or Vector3.new(0, 0.3, -2)
end

--------------------------------------------------------------------------------
-- PIVOTS
--
-- Every rotation applied to a weapon has to happen about a point, and the old
-- code applied all of them about the weapon root -- which, because the whole
-- viewmodel hung off the camera, was effectively the player's eyeball. A rifle
-- pivoting about your eye is a camera filter. A rifle pivoting about the butt
-- pad in your shoulder swings the muzzle through a metre-long arc while the
-- receiver under your eye barely moves, and that is what an object with mass
-- looks like.
--
-- POCKET  the butt pad where the stock meets your shoulder. Recoil rotates
--         about this. On a pistol there is no stock, so it is the web of the
--         firing hand, which is exactly where a handgun does pivot.
-- WRIST   the firing hand. Sway and inertia rotate about this, because that
--         is the joint your arm actually hangs the gun from.
-- REACH   studs from the root to the muzzle. Drives weapon collision.
--------------------------------------------------------------------------------
-- Measured off the converted mesh by tools/mk18_import.py, which prints them.
-- The downloaded model is 0.702 m (a correct 27.6 in MK18) scaled by 4.758
-- studs/m to match the viewmodel oversize the grip anchors were tuned to, which
-- puts its muzzle at z -2.153 and its butt pad at z +1.19.
-- Bore height above the grip, for the mesh path. muzzleOffset() reads the
-- PROCEDURAL model, which no longer matches the mesh -- so with the mesh loaded
-- the flash was being drawn 0.9 studs out in front of the barrel at the wrong
-- height. buildFromMesh uses (0, bore, -reach) instead.
Weapons.MK18.bore = 0.289
Weapons.MK18.pocket = Vector3.new(0.014, 0.286, 0.800)
Weapons.MK18.actionPivot = Vector3.new(0.014, 0.286, 0.50)
Weapons.MK18.wrist = Vector3.new(0, -0.036, 0.093)
Weapons.MK18.reach = 1.54

Weapons.M870.pocket = Vector3.new(0, 0.12, 1.18)
Weapons.M870.wrist = Vector3.new(0, -0.03, 0.15)
Weapons.M870.reach = 2.35

Weapons.P320.pocket = Vector3.new(0, -0.05, 0.14)
Weapons.P320.wrist = Vector3.new(0, -0.12, 0.06)
Weapons.P320.reach = 0.62

-- rot applied about a point instead of about the origin.
function Weapons.pivoted(p: Vector3, rot: CFrame): CFrame
	return CFrame.new(p) * rot * CFrame.new(-p)
end

--------------------------------------------------------------------------------
-- THE AIM POSE IS SOLVED, NOT AUTHORED.
--
-- It used to be a fixed translation: CFrame.new(ADS_EYE - sightOffset), applied
-- in the carry frame. That only lines the sights up if the carry frame happens
-- to be the camera -- which it no longer is, and more importantly which it
-- never really was once lean, crouch and torso lag were in play. Leaning while
-- aimed used to roll the camera and the weapon together so the sight picture
-- never changed, which is wrong: leaning should cost you the sight picture
-- until your body catches up.
--
-- So: solve for the weapon CFrame that puts the optical centre of the sight
-- exactly on the camera's optical axis at the correct eye relief, parallel to
-- the camera, and express it in the arm frame so it composes with everything
-- else in the chain. Correct under any lean, stance, torso lag or head lag,
-- for free, forever.
--------------------------------------------------------------------------------
function Weapons.adsSolve(w: Weapon, camCF: CFrame, armCF: CFrame, relief: number): CFrame
	local rot = camCF.Rotation
	local target = camCF.Position + camCF.LookVector * relief
	-- ADS_TRIM exists because sightOffset came off the mesh's bounding boxes,
	-- which give the sight PART's centre rather than its aperture. Tune it live.
	local offset = w.sightOffset + Cfg.VIEWMODEL.ADS_TRIM
	local desired = CFrame.new(target - rot:VectorToWorldSpace(offset)) * rot
	return armCF:ToObjectSpace(desired)
end

-- Spring rate for presenting the sights, derived from the weapon's own
-- adsTime rather than a global constant. A 3.6 kg shotgun and a 0.85 kg
-- pistol do not come up at the same speed, and a spring is what makes the
-- difference read as mass instead of as a different lerp coefficient -- it
-- accelerates, decelerates, and settles just past the mark.
-- `fatigue` is 0..1 arm stamina. Tired arms present the weapon more slowly,
-- which is the mechanical reason to ever use the low ready.
function Weapons.adsSpring(w: Weapon, fatigue: number?): (number, number)
	local t = math.max(w.adsTime, 0.05) * (1 + (fatigue or 0) * Cfg.ARM_STAMINA.ADS_PENALTY)
	local k = (Cfg.ADS.RATE / t) ^ 2
	return k, 2 * math.sqrt(k) * Cfg.ADS.DAMP
end

-- Total spread half-angle for a shot, in radians.
function Weapons.spread(w: Weapon, ads: boolean, moveFrac: number, bloom: number, stance: string): number
	local base = ads and w.spreadAds or w.spreadHip
	local s = base + w.spreadMove * moveFrac + bloom
	if stance == "crouch" then
		s *= 0.72
	end
	return s
end

-- Fuzzy magazine readout. A real operator does not get a number, they get a
-- feel for the weight and a look at the top of the stack.
function Weapons.describeMag(rounds: number, cap: number): string
	if rounds <= 0 then
		return "empty"
	end
	local f = rounds / cap
	if f >= 0.97 then
		return "full"
	elseif f >= 0.75 then
		return "most of a mag"
	elseif f >= 0.5 then
		return "over half"
	elseif f >= 0.3 then
		return "under half"
	elseif f >= 0.12 then
		return "low"
	end
	return "almost dry"
end

function Weapons.describeRig(mags: number): string
	if mags <= 0 then
		return "rig's empty"
	elseif mags == 1 then
		return "one left on the rig"
	elseif mags <= 3 then
		return "a couple on the rig"
	elseif mags <= 5 then
		return "rig's got a few"
	end
	return "rig's heavy"
end

Weapons.STARTING_LOADOUT = { "MK18", "P320" }
Weapons.ALL = { "MK18", "M870", "P320" }
Weapons.Cfg = Cfg

return Weapons
