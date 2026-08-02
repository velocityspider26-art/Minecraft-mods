--!nonstrict
-- MapBuilder -- generates the blacksite.
--
-- Everything is built from code at runtime; there is no hand-placed geometry in
-- the .rbxlx at all. The generator is a classic rooms-and-corridors carve on a
-- cell grid, which is worth the small amount of extra code because it is
-- connected *by construction*: every room is joined to the previous one before
-- the next is placed, so there is no way to generate a sealed-off objective.
--
-- Set GameConfig.MAP.SEED to a fixed number if you want the same layout every
-- run while you are tuning something.

local Cfg = require(script.Parent:WaitForChild("GameConfig"))
local MC = Cfg.MAP

local MapBuilder = {}

export type Room = { x: number, y: number, w: number, h: number, cx: number, cy: number, area: number }

export type MapData = {
	grid: { { number } },
	w: number,
	h: number,
	cell: number,
	rooms: { Room },
	folder: Folder,
	spawnCFrame: CFrame,
	patrol: { Vector3 },
	fuses: { BasePart },
	generator: BasePart,
	exit: BasePart,
	lights: { { light: PointLight | SpotLight, part: BasePart, emergency: boolean, flicker: boolean } },
	doors: { Model },
	cellToWorld: (number, number) -> Vector3,
	worldToCell: (Vector3) -> (number, number),
	isFloor: (number, number) -> boolean,
}

local CONCRETE = Color3.fromRGB(58, 59, 60)
local CONCRETE_DARK = Color3.fromRGB(40, 41, 43)
local PANEL = Color3.fromRGB(48, 52, 54)
local RUST = Color3.fromRGB(94, 63, 44)
local GRIME = Color3.fromRGB(31, 30, 28)

local function part(props: { [string]: any }, parent: Instance): BasePart
	local p = Instance.new("Part")
	p.Anchored = true
	p.TopSurface = Enum.SurfaceType.Smooth
	p.BottomSurface = Enum.SurfaceType.Smooth
	p.Material = Enum.Material.Concrete
	p.Color = CONCRETE
	for k, v in props do
		(p :: any)[k] = v
	end
	p.Parent = parent
	return p
end

--------------------------------------------------------------------------------
-- Grid generation
--------------------------------------------------------------------------------

local function newGrid(w: number, h: number): { { number } }
	local g = {}
	for y = 1, h do
		local row = {}
		for x = 1, w do
			row[x] = 0
		end
		g[y] = row
	end
	return g
end

local function carveRect(g, x1: number, y1: number, x2: number, y2: number, w: number, h: number)
	for y = math.max(1, y1), math.min(h, y2) do
		for x = math.max(1, x1), math.min(w, x2) do
			g[y][x] = 1
		end
	end
end

local function overlaps(a: Room, b: Room, margin: number): boolean
	return a.x - margin < b.x + b.w
		and a.x + a.w + margin > b.x
		and a.y - margin < b.y + b.h
		and a.y + a.h + margin > b.y
end

local function generateLayout(rng: Random): ({ { number } }, { Room })
	local w, h = MC.GRID_W, MC.GRID_H
	local g = newGrid(w, h)
	local rooms: { Room } = {}

	for _ = 1, MC.ROOM_TRIES do
		if #rooms >= MC.ROOM_COUNT then
			break
		end
		local rw = rng:NextInteger(MC.ROOM_MIN, MC.ROOM_MAX)
		local rh = rng:NextInteger(MC.ROOM_MIN, MC.ROOM_MAX)
		local rx = rng:NextInteger(2, w - rw - 1)
		local ry = rng:NextInteger(2, h - rh - 1)
		local cand: Room = {
			x = rx,
			y = ry,
			w = rw,
			h = rh,
			cx = rx + rw // 2,
			cy = ry + rh // 2,
			area = rw * rh,
		}

		local clear = true
		for _, other in rooms do
			if overlaps(cand, other, MC.ROOM_MARGIN) then
				clear = false
				break
			end
		end
		if clear then
			table.insert(rooms, cand)
		end
	end

	-- Carve rooms, then chain them together. Chaining in creation order is what
	-- guarantees the whole map is reachable.
	for _, r in rooms do
		carveRect(g, r.x, r.y, r.x + r.w - 1, r.y + r.h - 1, w, h)
	end

	local function corridor(ax: number, ay: number, bx: number, by: number)
		if rng:NextNumber() < 0.5 then
			carveRect(g, math.min(ax, bx), ay, math.max(ax, bx), ay, w, h)
			carveRect(g, bx, math.min(ay, by), bx, math.max(ay, by), w, h)
		else
			carveRect(g, ax, math.min(ay, by), ax, math.max(ay, by), w, h)
			carveRect(g, math.min(ax, bx), by, math.max(ax, bx), by, w, h)
		end
	end

	for i = 2, #rooms do
		corridor(rooms[i - 1].cx, rooms[i - 1].cy, rooms[i].cx, rooms[i].cy)
	end

	-- A pure tree of corridors means every fight is a dead end. Add loops.
	for _ = 1, MC.EXTRA_LOOPS do
		if #rooms < 3 then
			break
		end
		local a = rooms[rng:NextInteger(1, #rooms)]
		local b = rooms[rng:NextInteger(1, #rooms)]
		if a ~= b then
			corridor(a.cx, a.cy, b.cx, b.cy)
		end
	end

	return g, rooms
end

--------------------------------------------------------------------------------
-- Geometry
--------------------------------------------------------------------------------

-- Only build a wall where the player could ever see one: a solid cell touching
-- a floor cell. The rest of the grid stays as implied bedrock and costs nothing.
local function exposedWalls(g, w: number, h: number): { { boolean } }
	local mask = {}
	for y = 1, h do
		mask[y] = {}
		for x = 1, w do
			local exposed = false
			if g[y][x] == 0 then
				for dy = -1, 1 do
					for dx = -1, 1 do
						local ny, nx = y + dy, x + dx
						if ny >= 1 and ny <= h and nx >= 1 and nx <= w and g[ny][nx] == 1 then
							exposed = true
						end
					end
				end
			end
			mask[y][x] = exposed
		end
	end
	return mask
end

function MapBuilder.build(seedOverride: number?): MapData
	local seed = seedOverride or MC.SEED
	if seed == 0 then
		seed = math.floor(os.clock() * 1e6) % 2147483647
	end
	local rng = Random.new(seed)

	local g, rooms = generateLayout(rng)
	local w, h, cell = MC.GRID_W, MC.GRID_H, MC.CELL

	local originX = -(w * cell) * 0.5
	local originZ = -(h * cell) * 0.5

	local function cellToWorld(cx: number, cy: number): Vector3
		return Vector3.new(originX + (cx - 0.5) * cell, 0, originZ + (cy - 0.5) * cell)
	end
	local function worldToCell(v: Vector3): (number, number)
		return math.floor((v.X - originX) / cell) + 1, math.floor((v.Z - originZ) / cell) + 1
	end
	local function isFloor(cx: number, cy: number): boolean
		if cx < 1 or cx > w or cy < 1 or cy > h then
			return false
		end
		return g[cy][cx] == 1
	end

	local old = workspace:FindFirstChild("Facility")
	if old then
		old:Destroy()
	end

	local folder = Instance.new("Folder")
	folder.Name = "Facility"
	folder.Parent = workspace

	local structure = Instance.new("Folder")
	structure.Name = "Structure"
	structure.Parent = folder
	local props = Instance.new("Folder")
	props.Name = "Props"
	props.Parent = folder
	local lightFolder = Instance.new("Folder")
	lightFolder.Name = "Lights"
	lightFolder.Parent = folder
	local doorFolder = Instance.new("Folder")
	doorFolder.Name = "Doors"
	doorFolder.Parent = folder
	local objectives = Instance.new("Folder")
	objectives.Name = "Objectives"
	objectives.Parent = folder

	-- Slab floor and ceiling. Two parts for the entire level.
	part({
		Name = "Slab",
		Size = Vector3.new(w * cell, MC.FLOOR_THICK, h * cell),
		CFrame = CFrame.new(0, -MC.FLOOR_THICK * 0.5, 0),
		Color = CONCRETE_DARK,
		Material = Enum.Material.Concrete,
	}, structure)

	part({
		Name = "Ceiling",
		Size = Vector3.new(w * cell, MC.CEILING_THICK, h * cell),
		CFrame = CFrame.new(0, MC.WALL_HEIGHT + MC.CEILING_THICK * 0.5, 0),
		Color = GRIME,
		Material = Enum.Material.Slate,
	}, structure)

	-- Walls, merged into horizontal runs so 800-odd cells become ~250 parts.
	local mask = exposedWalls(g, w, h)
	for y = 1, h do
		local x = 1
		while x <= w do
			if mask[y][x] then
				local run = 0
				while x + run <= w and mask[y][x + run] do
					run += 1
				end
				local midX = originX + (x - 1 + run * 0.5) * cell
				local midZ = originZ + (y - 0.5) * cell
				part({
					Name = "Wall",
					Size = Vector3.new(run * cell, MC.WALL_HEIGHT, cell),
					CFrame = CFrame.new(midX, MC.WALL_HEIGHT * 0.5, midZ),
					Color = rng:NextNumber() < 0.25 and PANEL or CONCRETE,
					Material = rng:NextNumber() < 0.3 and Enum.Material.Metal or Enum.Material.Concrete,
				}, structure)
				x += run
			else
				x += 1
			end
		end
	end

	--------------------------------------------------------------------------
	-- Rooms: pick roles, then dress them
	--------------------------------------------------------------------------
	table.sort(rooms, function(a, b)
		return a.area > b.area
	end)

	-- The generator gets the biggest room; start and exit want to be as far
	-- apart as the layout allows. With fewer than four rooms the roles double
	-- up rather than erroring -- the generator is tuned to make that very
	-- unlikely, but a bad seed should still produce a playable level.
	local generatorRoom = rooms[1]
	local startRoom = rooms[math.min(2, #rooms)]
	local exitRoom = rooms[math.min(3, #rooms)]
	local bestDist = -1
	for i = 2, #rooms do
		for j = 2, #rooms do
			if i ~= j then
				local a, b = rooms[i], rooms[j]
				local d = (a.cx - b.cx) ^ 2 + (a.cy - b.cy) ^ 2
				if d > bestDist then
					bestDist, startRoom, exitRoom = d, a, b
				end
			end
		end
	end

	local used = { [generatorRoom] = true, [startRoom] = true, [exitRoom] = true }
	local fuseRooms: { Room } = {}
	for _, r in rooms do
		if #fuseRooms >= MC.FUSE_COUNT then
			break
		end
		if not used[r] then
			table.insert(fuseRooms, r)
			used[r] = true
		end
	end

	--------------------------------------------------------------------------
	-- Lighting fixtures
	--------------------------------------------------------------------------
	local lights = {}
	local function fixture(pos: Vector3, emergency: boolean, prelit: boolean)
		-- CastShadow = false on the housing AND the element is not cosmetic.
		-- Under Future lighting a SpotLight is occluded by geometry in front
		-- of it, and both of these sit directly between the bulb and the
		-- floor -- leave shadows on and the fixture lights nothing at all.
		local housing = part({
			Name = "Fixture",
			Size = Vector3.new(2.8, 0.35, 1.0),
			CFrame = CFrame.new(pos.X, MC.WALL_HEIGHT - 0.4, pos.Z),
			Color = emergency and Color3.fromRGB(40, 12, 10) or Color3.fromRGB(22, 23, 24),
			Material = Enum.Material.Metal,
			CanCollide = false,
			CanQuery = false,
			CastShadow = false,
		}, lightFolder)

		-- The glowing element itself. Without this the fixture is a dark box
		-- with light coming out of nowhere -- you need to be able to see which
		-- fixture is lit from across a room, and it is what the bloom catches.
		local tube = part({
			Name = "Element",
			Size = Vector3.new(2.3, 0.10, 0.55),
			CFrame = CFrame.new(pos.X, MC.WALL_HEIGHT - 0.60, pos.Z),
			Color = emergency and MC.EMERGENCY_COLOR or MC.LIGHT_TUBE_COLOR,
			Material = Enum.Material.Neon,
			CanCollide = false,
			CanQuery = false,
			CastShadow = false,
		}, housing)

		-- Some ordinary fixtures run off the emergency circuit and are lit from
		-- the start, dimly. A facility that is 100% black until you finish the
		-- objective gives the player nothing to navigate by, and "dim ceiling
		-- lights" is the look we are after anyway.
		local on = emergency or prelit

		local bulb = Instance.new("SpotLight")
		bulb.Face = Enum.NormalId.Bottom
		bulb.Angle = MC.LIGHT_ANGLE
		bulb.Range = emergency and MC.EMERGENCY_RANGE or MC.LIGHT_RANGE
		bulb.Color = emergency and MC.EMERGENCY_COLOR or MC.LIGHT_COLOR
		bulb.Brightness = emergency and MC.EMERGENCY_BRIGHTNESS
			or (prelit and MC.LIGHT_BRIGHTNESS * MC.PRELIT_DIM or MC.LIGHT_BRIGHTNESS)
		bulb.Shadows = true
		bulb.Enabled = on
		bulb.Parent = housing

		tube.Transparency = on and 0 or 0.85

		-- Flicker is driven entirely by each client so it costs no network
		-- traffic; the server only tags which fixtures are faulty.
		local flicker = rng:NextNumber() < MC.FLICKER_CHANCE
		housing:SetAttribute("Emergency", emergency)
		housing:SetAttribute("Flicker", flicker)

		table.insert(lights, {
			light = bulb,
			part = housing,
			element = tube,
			emergency = emergency,
			flicker = flicker,
		})
	end

	for _, r in rooms do
		local n = math.max(1, math.floor(r.area / 26))
		for i = 1, n do
			local lx = r.x + rng:NextInteger(1, math.max(1, r.w - 1))
			local ly = r.y + rng:NextInteger(1, math.max(1, r.h - 1))
			fixture(
				cellToWorld(lx, ly),
				rng:NextNumber() < MC.EMERGENCY_LIGHT_CHANCE,
				rng:NextNumber() < MC.PRELIT_CHANCE
			)
		end
	end
	for y = 1, h, MC.LIGHT_CORRIDOR_EVERY do
		for x = 1, w, MC.LIGHT_CORRIDOR_EVERY do
			if isFloor(x, y) then
				fixture(
					cellToWorld(x, y),
					rng:NextNumber() < MC.EMERGENCY_LIGHT_CHANCE * 1.6,
					rng:NextNumber() < MC.PRELIT_CHANCE
				)
			end
		end
	end

	--------------------------------------------------------------------------
	-- Doors on room openings
	--------------------------------------------------------------------------
	local doors: { Model } = {}
	local function makeDoor(cx: number, cy: number, horizontal: boolean)
		local pos = cellToWorld(cx, cy)
		local model = Instance.new("Model")
		model.Name = "Door"
		model.Parent = doorFolder

		-- The leaf starts open, which means it must be invisible to raycasts as
		-- well as to physics -- otherwise an open doorway would silently stop
		-- bullets. CanQuery is toggled with CanCollide in GameServer.setDoor.
		local size = horizontal and Vector3.new(cell, MC.WALL_HEIGHT - 1, 0.45)
			or Vector3.new(0.45, MC.WALL_HEIGHT - 1, cell)
		local leaf = part({
			Name = "Leaf",
			Size = size,
			CFrame = CFrame.new(pos.X, (MC.WALL_HEIGHT - 1) * 0.5, pos.Z),
			Color = Color3.fromRGB(66, 62, 56),
			Material = Enum.Material.DiamondPlate,
			CanCollide = false,
			CanQuery = false,
			Transparency = 1,
		}, model)
		model.PrimaryPart = leaf

		-- Header above the opening. Decoration only.
		part({
			Name = "Frame",
			Size = horizontal and Vector3.new(cell + 0.6, 0.6, 1.2) or Vector3.new(1.2, 0.6, cell + 0.6),
			CFrame = CFrame.new(pos.X, MC.WALL_HEIGHT - 1.2, pos.Z),
			Color = RUST,
			Material = Enum.Material.CorrodedMetal,
			CanCollide = false,
			CanQuery = false,
		}, model)

		-- The interact target is a wall panel beside the opening, not the leaf,
		-- so that closing a door works the same whether it is open or shut.
		local off = horizontal and Vector3.new(cell * 0.5 - 0.25, 0, 0.8)
			or Vector3.new(0.8, 0, cell * 0.5 - 0.25)
		local panel = part({
			Name = "Panel",
			Size = Vector3.new(0.7, 1.0, 0.7),
			CFrame = CFrame.new(pos.X + off.X, 3.6, pos.Z + off.Z),
			Color = Color3.fromRGB(78, 74, 62),
			Material = Enum.Material.Metal,
			CanCollide = false,
		}, model)
		panel:SetAttribute("Interact", "DOOR")

		model:SetAttribute("Closed", false)
		model:SetAttribute("Horizontal", horizontal)
		table.insert(doors, model)
	end

	for _, r in rooms do
		local placed = 0
		for x = r.x - 1, r.x + r.w do
			for _, y in { r.y - 1, r.y + r.h } do
				if placed < 3 and isFloor(x, y) and x >= r.x and x < r.x + r.w then
					makeDoor(x, y, true)
					placed += 1
				end
			end
		end
		for y = r.y, r.y + r.h - 1 do
			for _, x in { r.x - 1, r.x + r.w } do
				if placed < 3 and isFloor(x, y) then
					makeDoor(x, y, false)
					placed += 1
				end
			end
		end
	end

	--------------------------------------------------------------------------
	-- Clutter
	--------------------------------------------------------------------------
	local function clutter(r: Room)
		for _ = 1, MC.CLUTTER_PER_ROOM do
			local cx = rng:NextInteger(r.x, r.x + r.w - 1)
			local cy = rng:NextInteger(r.y, r.y + r.h - 1)
			local pos = cellToWorld(cx, cy)
			local roll = rng:NextNumber()
			local yaw = rng:NextNumber(0, math.pi * 2)

			if roll < 0.32 then -- crate stack
				local hgt = rng:NextInteger(1, 3)
				for i = 1, hgt do
					part({
						Name = "Crate",
						Size = Vector3.new(2.8, 2.2, 2.8),
						CFrame = CFrame.new(pos.X, 1.1 + (i - 1) * 2.2, pos.Z) * CFrame.Angles(0, yaw, 0),
						Color = rng:NextNumber() < 0.5 and Color3.fromRGB(72, 66, 50) or Color3.fromRGB(46, 52, 46),
						Material = Enum.Material.WoodPlanks,
					}, props)
				end
			elseif roll < 0.55 then -- shelving
				for i = 0, 2 do
					part({
						Name = "Shelf",
						Size = Vector3.new(5.2, 0.25, 1.6),
						CFrame = CFrame.new(pos.X, 1.2 + i * 1.8, pos.Z) * CFrame.Angles(0, yaw, 0),
						Color = Color3.fromRGB(52, 54, 56),
						Material = Enum.Material.Metal,
					}, props)
				end
			elseif roll < 0.70 then -- barrel
				-- A Roblox cylinder runs along its X axis, so the drum height
				-- goes in Size.X and the part is rolled 90 degrees to stand up.
				local b = part({
					Name = "Barrel",
					Size = Vector3.new(2.8, 1.9, 1.9),
					CFrame = CFrame.new(pos.X, 1.4, pos.Z) * CFrame.Angles(0, yaw, math.rad(90)),
					Color = RUST,
					Material = Enum.Material.CorrodedMetal,
				}, props)
				b.Shape = Enum.PartType.Cylinder
			elseif roll < 0.84 then -- desk
				part({
					Name = "Desk",
					Size = Vector3.new(4.4, 0.2, 2.2),
					CFrame = CFrame.new(pos.X, 2.0, pos.Z) * CFrame.Angles(0, yaw, 0),
					Color = Color3.fromRGB(60, 58, 54),
					Material = Enum.Material.Metal,
				}, props)
			else -- dried blood
				part({
					Name = "Stain",
					Size = Vector3.new(rng:NextNumber(3, 7), 0.06, rng:NextNumber(3, 7)),
					CFrame = CFrame.new(pos.X, 0.03, pos.Z) * CFrame.Angles(0, yaw, 0),
					Color = Color3.fromRGB(42, 12, 10),
					Material = Enum.Material.Slate,
					CanCollide = false,
				}, props)
			end
		end
	end
	for _, r in rooms do
		clutter(r)
	end

	-- Ceiling pipes down the long corridors, purely so the dark has depth.
	for y = 3, h - 2, 3 do
		local runStart = nil
		for x = 1, w + 1 do
			if isFloor(x, y) then
				runStart = runStart or x
			elseif runStart then
				local len = x - runStart
				if len >= 5 then
					part({
						Name = "Pipe",
						Size = Vector3.new(len * cell, 0.55, 0.55),
						CFrame = CFrame.new(
							originX + (runStart - 1 + len * 0.5) * cell,
							MC.WALL_HEIGHT - 1.4,
							originZ + (y - 0.5) * cell + 2.2
						),
						Color = Color3.fromRGB(58, 50, 44),
						Material = Enum.Material.CorrodedMetal,
						CanCollide = false,
					}, props)
				end
				runStart = nil
			end
		end
	end

	--------------------------------------------------------------------------
	-- Objectives
	--------------------------------------------------------------------------
	local function marker(name: string, pos: Vector3, size: Vector3, color: Color3, tag: string): BasePart
		local p = part({
			Name = name,
			Size = size,
			CFrame = CFrame.new(pos.X, size.Y * 0.5 + 0.1, pos.Z),
			Color = color,
			Material = Enum.Material.Metal,
			CanCollide = false,
		}, objectives)
		p:SetAttribute("Interact", tag)

		local glow = Instance.new("PointLight")
		glow.Color = color
		glow.Brightness = 1.4
		glow.Range = 12
		glow.Parent = p
		return p
	end

	local fuses: { BasePart } = {}
	for i, r in fuseRooms do
		local pos = cellToWorld(r.cx, r.cy)
		local f = marker("PowerCell", pos, Vector3.new(1.2, 1.8, 0.9), Color3.fromRGB(70, 190, 240), "FUSE")
		f:SetAttribute("Index", i)
		table.insert(fuses, f)
	end

	local genPos = cellToWorld(generatorRoom.cx, generatorRoom.cy)
	part({
		Name = "GeneratorBody",
		Size = Vector3.new(7.5, 5.0, 4.2),
		CFrame = CFrame.new(genPos.X, 2.5, genPos.Z),
		Color = Color3.fromRGB(64, 68, 62),
		Material = Enum.Material.DiamondPlate,
	}, objectives)
	local generator = marker(
		"GeneratorPanel",
		genPos + Vector3.new(0, 0, 3.0),
		Vector3.new(3.0, 2.2, 0.5),
		Color3.fromRGB(240, 170, 60),
		"GENERATOR"
	)
	generator.CFrame = CFrame.new(genPos.X, 3.2, genPos.Z + 2.4)

	local exitPos = cellToWorld(exitRoom.cx, exitRoom.cy)
	part({
		Name = "ElevatorShaft",
		Size = Vector3.new(9, MC.WALL_HEIGHT, 6),
		CFrame = CFrame.new(exitPos.X, MC.WALL_HEIGHT * 0.5, exitPos.Z),
		Color = Color3.fromRGB(36, 38, 40),
		Material = Enum.Material.Metal,
		CanCollide = false,
		CanQuery = false, -- must not shadow the call button behind it
		Transparency = 0.75,
	}, objectives)
	local exitPart = marker(
		"ElevatorCall",
		exitPos,
		Vector3.new(2.0, 2.6, 0.5),
		Color3.fromRGB(90, 220, 120),
		"EXIT"
	)
	exitPart.CFrame = CFrame.new(exitPos.X + 3.4, 3.0, exitPos.Z)

	--------------------------------------------------------------------------
	-- Spawn + patrol graph
	--------------------------------------------------------------------------
	local startPos = cellToWorld(startRoom.cx, startRoom.cy)
	local spawnLoc = Instance.new("SpawnLocation")
	spawnLoc.Name = "Insert"
	spawnLoc.Size = Vector3.new(10, 1, 10)
	spawnLoc.CFrame = CFrame.new(startPos.X, 0.5, startPos.Z)
	spawnLoc.Anchored = true
	spawnLoc.Neutral = true
	spawnLoc.Duration = 0
	spawnLoc.CanCollide = true
	spawnLoc.Color = Color3.fromRGB(38, 44, 38)
	spawnLoc.Material = Enum.Material.DiamondPlate
	spawnLoc.Transparency = 0
	spawnLoc.Parent = folder

	local patrol: { Vector3 } = {}
	for _, r in rooms do
		table.insert(patrol, cellToWorld(r.cx, r.cy) + Vector3.new(0, 3, 0))
	end
	-- Corridor nodes too, so it does not only ever walk room to room.
	for y = 2, h - 1, 4 do
		for x = 2, w - 1, 4 do
			if isFloor(x, y) and rng:NextNumber() < 0.4 then
				table.insert(patrol, cellToWorld(x, y) + Vector3.new(0, 3, 0))
			end
		end
	end

	return {
		grid = g,
		w = w,
		h = h,
		cell = cell,
		rooms = rooms,
		folder = folder,
		spawnCFrame = CFrame.new(startPos + Vector3.new(0, 4, 0)),
		patrol = patrol,
		fuses = fuses,
		generator = generator,
		exit = exitPart,
		lights = lights,
		doors = doors,
		cellToWorld = cellToWorld,
		worldToCell = worldToCell,
		isFloor = isFloor,
	}
end

return MapBuilder
