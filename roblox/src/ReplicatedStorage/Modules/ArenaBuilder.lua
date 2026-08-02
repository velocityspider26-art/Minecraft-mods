--!nonstrict
-- ArenaBuilder -- a daylight test box for working on PVP.
--
-- This is NOT the facility. It exists so weapon feel, the stance system and
-- player-versus-player damage can be worked on without the dark, the entity and
-- the objective chain in the way. Cfg.MODE picks which one the server builds;
-- MapBuilder is untouched and the facility still works.
--
-- It returns the same shape MapBuilder does -- empty fuses/doors/lights, dummy
-- generator and exit -- so GameServer only has to skip the entity and the
-- lighting, not branch on every field.
--
-- DELIBERATE: the layout is fixed, not seeded. A test map that reshuffles every
-- run is useless for judging whether a change to lean or crouch helped.
--
-- COVER HEIGHTS are the whole point of the prop set. With the current rig the
-- standing eye is ~5.5 studs off the deck and the crouched eye ~4.1, so:
--     2.6 studs  visible either way -- shoot over it standing or crouched
--     4.6 studs  THE interesting one: exposed standing, hidden crouched
--     5.6 studs  hidden standing, must lean or step out to shoot
--     7.4 studs  full cover, corner work only
-- Analog crouch (hold C + mouse) is the mechanic being exercised, so there is
-- at least one run of every height.

local Cfg = require(script.Parent:WaitForChild("GameConfig"))

local Arena = {}

local CONCRETE = Color3.fromRGB(126, 126, 122)
local CONCRETE_DARK = Color3.fromRGB(96, 97, 95)
local SAND = Color3.fromRGB(150, 138, 108)
local RUST = Color3.fromRGB(126, 78, 52)
local CONTAINER_A = Color3.fromRGB(58, 84, 96)
local CONTAINER_B = Color3.fromRGB(96, 74, 52)
local STEEL = Color3.fromRGB(104, 108, 112)
local GRASS = Color3.fromRGB(88, 104, 68)

local function part(props: { [string]: any }, parent: Instance): BasePart
	local p = Instance.new("Part")
	p.Anchored = true
	p.TopSurface = Enum.SurfaceType.Smooth
	p.BottomSurface = Enum.SurfaceType.Smooth
	for k, v in props do
		(p :: any)[k] = v
	end
	p.Parent = parent
	return p
end

-- A block of cover sitting ON the ground: pass the height and it works out the
-- centre, so every number in the layout below is a real-world height.
local function cover(
	parent: Instance,
	name: string,
	x: number,
	z: number,
	sx: number,
	height: number,
	sz: number,
	color: Color3,
	mat: Enum.Material,
	yaw: number?
)
	return part({
		Name = name,
		Size = Vector3.new(sx, height, sz),
		CFrame = CFrame.new(x, height * 0.5, z) * CFrame.Angles(0, math.rad(yaw or 0), 0),
		Color = color,
		Material = mat,
	}, parent)
end

function Arena.build()
	local A = Cfg.ARENA
	local half = A.SIZE * 0.5

	local folder = Instance.new("Folder")
	folder.Name = "Facility" -- kept so Atmosphere's light indexer still finds it
	folder.Parent = workspace

	local props = Instance.new("Folder")
	props.Name = "Cover"
	props.Parent = folder

	-- Atmosphere walks Facility/Lights; in daylight there are none, but the
	-- folder has to exist or the indexer spins forever waiting for it.
	local lightsFolder = Instance.new("Folder")
	lightsFolder.Name = "Lights"
	lightsFolder.Parent = folder

	----------------------------------------------------------------------------
	-- Ground and perimeter
	----------------------------------------------------------------------------
	part({
		Name = "Baseplate",
		Size = Vector3.new(A.SIZE, 4, A.SIZE),
		CFrame = CFrame.new(0, -2, 0),
		Color = GRASS,
		Material = Enum.Material.Grass,
	}, folder)

	-- A concrete pad in the middle so the centre of the map reads as built-up
	-- rather than a field with boxes on it.
	part({
		Name = "Pad",
		Size = Vector3.new(A.PAD, 0.4, A.PAD),
		CFrame = CFrame.new(0, 0.2, 0),
		Color = CONCRETE_DARK,
		Material = Enum.Material.Concrete,
	}, folder)

	for i, s in { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } } do
		local sx = s[1] ~= 0 and 2 or A.SIZE
		local sz = s[2] ~= 0 and 2 or A.SIZE
		cover(
			folder,
			"Perimeter" .. i,
			s[1] * half,
			s[2] * half,
			sx,
			A.WALL_HEIGHT,
			sz,
			CONCRETE_DARK,
			Enum.Material.Concrete
		)
	end

	----------------------------------------------------------------------------
	-- Centre building. Four walls, two doorways, no roof -- a corner-work box.
	-- The doorways are what the continuous weapon-collision system is for:
	-- walking a muzzle through a 6-stud gap should push the gun up.
	----------------------------------------------------------------------------
	local b = A.BUILDING
	local bh = 7.4
	-- North and south walls, each split to leave a doorway in the middle.
	for _, sz in { -1, 1 } do
		local seg = (b - A.DOOR_WIDTH) * 0.5
		for _, sx in { -1, 1 } do
			cover(
				props,
				"BuildingWall",
				sx * (A.DOOR_WIDTH + seg) * 0.5,
				sz * b * 0.5,
				seg,
				bh,
				1.4,
				CONCRETE,
				Enum.Material.Concrete
			)
		end
	end
	-- East and west walls, solid.
	for _, sx in { -1, 1 } do
		cover(props, "BuildingWall", sx * b * 0.5, 0, 1.4, bh, b, CONCRETE, Enum.Material.Concrete)
	end
	-- Interior pillar: something to lean around inside the box.
	cover(props, "Pillar", 0, 0, 2.4, bh, 2.4, CONCRETE_DARK, Enum.Material.Concrete)

	----------------------------------------------------------------------------
	-- Cover field. Laid out with 180-degree rotational symmetry so neither
	-- spawn has the better angles -- every entry is placed twice, mirrored
	-- through the origin.
	----------------------------------------------------------------------------
	-- {x, z, sizeX, height, sizeZ, colour, material, yaw}
	local layout = {
		-- Shipping containers. 12 m long, 2.6 m tall = 34 x 7.4 studs.
		{ -46, 38, 34, 7.4, 6.9, CONTAINER_A, Enum.Material.CorrodedMetal, 0 },
		{ 52, 62, 34, 7.4, 6.9, CONTAINER_B, Enum.Material.CorrodedMetal, 90 },
		{ -74, -18, 34, 7.4, 6.9, CONTAINER_B, Enum.Material.CorrodedMetal, 22 },

		-- Jersey barriers, 0.9 m = 2.6 studs. Waist height: you can shoot over
		-- these from a crouch, which is what makes them worth using.
		{ -22, 66, 9, 2.6, 2.2, CONCRETE, Enum.Material.Concrete, 0 },
		{ -12, 66, 9, 2.6, 2.2, CONCRETE, Enum.Material.Concrete, 0 },
		{ -2, 66, 9, 2.6, 2.2, CONCRETE, Enum.Material.Concrete, 0 },
		{ 40, 22, 9, 2.6, 2.2, CONCRETE, Enum.Material.Concrete, 90 },

		-- Sandbags at 4.6. THE height that makes analog crouch matter: your
		-- head is over it standing and behind it crouched.
		{ -60, 8, 14, 4.6, 3.4, SAND, Enum.Material.Sand, 0 },
		{ 26, 46, 14, 4.6, 3.4, SAND, Enum.Material.Sand, 0 },
		{ 8, -34, 18, 4.6, 3.4, SAND, Enum.Material.Sand, 74 },

		-- 5.6: hidden standing, so you have to lean or break cover to shoot.
		{ -34, -56, 16, 5.6, 2.6, CONCRETE, Enum.Material.Concrete, 0 },
		{ 62, -8, 16, 5.6, 2.6, CONCRETE, Enum.Material.Concrete, 90 },

		-- Crate stacks at mixed heights, for everything in between.
		{ 18, 78, 6, 3.2, 6, RUST, Enum.Material.WoodPlanks, 12 },
		{ 24, 72, 5, 6.4, 5, RUST, Enum.Material.WoodPlanks, -8 },
		{ -88, 46, 6, 4.8, 6, RUST, Enum.Material.WoodPlanks, 30 },

		-- Freestanding pillars: pure lean practice, no body to hide behind.
		{ 84, 34, 3, 7.4, 3, CONCRETE_DARK, Enum.Material.Concrete, 0 },
		{ -104, -40, 3, 7.4, 3, CONCRETE_DARK, Enum.Material.Concrete, 0 },
	}

	for _, e in layout do
		for _, m in { 1, -1 } do
			cover(props, "Cover", e[1] * m, e[2] * m, e[3], e[4], e[5], e[6], e[7], (e[8] or 0) + (m < 0 and 180 or 0))
		end
	end

	----------------------------------------------------------------------------
	-- Raised platforms with ramps. Elevation changes are the one thing a flat
	-- box cannot test, and they are where the landing dip and the step impulses
	-- actually show up.
	----------------------------------------------------------------------------
	for _, m in { 1, -1 } do
		local px, pz = 96 * m, -74 * m
		cover(props, "Platform", px, pz, 26, A.PLATFORM_HEIGHT, 26, CONCRETE, Enum.Material.Concrete)
		-- Ramp: a rotated slab is enough to walk up and reads correctly.
		local rampLen = A.PLATFORM_HEIGHT * 3.2
		local ang = math.atan(A.PLATFORM_HEIGHT / rampLen)
		part({
			Name = "Ramp",
			Size = Vector3.new(10, 1, math.sqrt(rampLen ^ 2 + A.PLATFORM_HEIGHT ^ 2)),
			CFrame = CFrame.new(px, A.PLATFORM_HEIGHT * 0.5, pz + (13 + rampLen * 0.5) * m)
				* CFrame.Angles(0, m < 0 and math.pi or 0, 0)
				* CFrame.Angles(ang, 0, 0),
			Color = CONCRETE_DARK,
			Material = Enum.Material.Concrete,
		}, props)
	end

	----------------------------------------------------------------------------
	-- Zeroing range down one edge. Steel plates at marked distances, so "does
	-- this weapon shoot where the sight says" is a five-second check instead of
	-- a feeling. Distances are in METRES on the label and studs in the world.
	----------------------------------------------------------------------------
	local lineZ = -half + 16
	part({
		Name = "FiringLine",
		Size = Vector3.new(A.SIZE * 0.7, 0.3, 3),
		CFrame = CFrame.new(0, 0.25, lineZ),
		Color = Color3.fromRGB(180, 168, 60),
		Material = Enum.Material.Concrete,
	}, folder)

	local range = Instance.new("Folder")
	range.Name = "Range"
	range.Parent = folder

	for _, metres in A.RANGE_MARKS do
		local d = metres * Cfg.STUDS_PER_METER
		if lineZ + d < half - 8 then
			local plate = part({
				Name = ("Plate_%dm"):format(metres),
				Size = Vector3.new(3.4, 5.2, 0.5),
				CFrame = CFrame.new(-30 + (metres % 3) * 30, 2.6 + 1.2, lineZ + d),
				Color = STEEL,
				Material = Enum.Material.Metal,
			}, range)
			-- Post, so the plate is not floating.
			cover(range, "Post", plate.Position.X, plate.Position.Z, 0.8, 2.6, 0.8, STEEL, Enum.Material.Metal)

			local sign = Instance.new("BillboardGui")
			sign.Size = UDim2.fromOffset(90, 26)
			sign.StudsOffsetWorldSpace = Vector3.new(0, 3.6, 0)
			sign.AlwaysOnTop = false
			sign.MaxDistance = 260
			sign.Parent = plate

			local label = Instance.new("TextLabel")
			label.Size = UDim2.fromScale(1, 1)
			label.BackgroundTransparency = 1
			label.Font = Enum.Font.Code
			label.TextSize = 18
			label.TextColor3 = Color3.fromRGB(240, 236, 210)
			label.TextStrokeTransparency = 0.4
			label.Text = ("%d m"):format(metres)
			label.Parent = sign
		end
	end

	----------------------------------------------------------------------------
	-- Spawns. Real SpawnLocations, so Roblox handles spawn and respawn and
	-- there is no teleport code to keep in sync.
	----------------------------------------------------------------------------
	local spawns: { BasePart } = {}
	for i, s in A.SPAWNS do
		local loc = Instance.new("SpawnLocation")
		loc.Name = "Spawn" .. i
		loc.Size = Vector3.new(12, 1, 12)
		loc.CFrame = CFrame.new(s[1], 0.5, s[2]) * CFrame.Angles(0, math.rad(s[3]), 0)
		loc.Anchored = true
		loc.Neutral = true -- free-for-all; give them TeamColor when teams exist
		loc.Duration = 0 -- no forcefield: this is a PVP test box
		loc.CanCollide = true
		loc.Color = Color3.fromRGB(70, 82, 70)
		loc.Material = Enum.Material.DiamondPlate
		loc.Parent = folder
		table.insert(spawns, loc)
	end

	----------------------------------------------------------------------------
	-- Dummy objective props. GameServer reads map.generator and map.exit
	-- unconditionally; these exist so nothing has to nil-check, and they are
	-- tagged with no Interact attribute so they do nothing.
	----------------------------------------------------------------------------
	local dummy = part({
		Name = "Unused",
		Size = Vector3.one,
		CFrame = CFrame.new(0, -40, 0),
		Transparency = 1,
		CanCollide = false,
		CanQuery = false,
	}, folder)

	local patrol: { Vector3 } = {}
	for _, s in A.SPAWNS do
		table.insert(patrol, Vector3.new(s[1], 3, s[2]))
	end

	return {
		arena = true,
		grid = {},
		w = 0,
		h = 0,
		cell = Cfg.MAP.CELL,
		rooms = {},
		folder = folder,
		spawnCFrame = CFrame.new(A.SPAWNS[1][1], 4, A.SPAWNS[1][2]),
		patrol = patrol,
		fuses = {},
		generator = dummy,
		exit = dummy,
		lights = {},
		doors = {},
		spawns = spawns,
		cellToWorld = function(_x: number, _y: number)
			return Vector3.zero
		end,
		worldToCell = function(_p: Vector3)
			return 0, 0
		end,
		isFloor = function(_x: number, _y: number)
			return true
		end,
	}
end

return Arena
