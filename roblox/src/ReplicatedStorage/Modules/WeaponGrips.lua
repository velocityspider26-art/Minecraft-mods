--!nonstrict
-- WeaponGrips -- where a real hand goes on a weapon, derived from the weapon.
--
-- The grip anchors in Weapons.luau were authored for the first-person viewmodel,
-- whose "hands" are a ball and some finger boxes. Orientation barely mattered
-- there. It matters completely for a real hand with real knuckles: a palm frame
-- rolled 90 degrees closes the fingers through the grip instead of around it.
--
-- Rather than hand-tune a rotation per weapon and hope, this measures the grip
-- the same way roblox/tools/convert_weapon.py measures the imported rifle's:
--
--   X   up the grip's long axis, toward the trigger end
--   Z   out of the face the fingers wrap onto, i.e. toward the muzzle
--
-- A pistol grip is a long box tilted back a few degrees; its long axis is its
-- own local Y. That single fact is enough to build the frame, so every weapon
-- in the game -- procedural or imported -- gets a correct hand without anyone
-- typing a quaternion.

local WeaponGrips = {}

local FORWARD = Vector3.new(0, 0, -1) -- the bore, in weapon-local space
local UP = Vector3.new(0, 1, 0)

-- Which model part is which grip. First match wins, so the more specific names
-- come first.
local RIGHT_NAMES = {"PistolGrip", "Grip", "GripBase"}
local LEFT_NAMES = {"Foregrip", "ForeGrip", "VerticalGrip", "HandguardBody",
	"Handguard", "Pump", "SlideGrip"}

local function frame(origin: Vector3, axis: Vector3, normalHint: Vector3): CFrame
	local x = axis.Magnitude > 1e-5 and axis.Unit or UP
	local n = normalHint - x * normalHint:Dot(x)
	if n.Magnitude < 1e-5 then
		n = x:Cross(Vector3.xAxis)
		if n.Magnitude < 1e-5 then n = x:Cross(Vector3.zAxis) end
	end
	n = n.Unit
	local y = n:Cross(x)
	return CFrame.fromMatrix(origin, x, y, n)
end

WeaponGrips.frame = frame

local function partCFrame(entry: any): CFrame
	local pos = entry.pos or Vector3.zero
	local rot = entry.rot
	local cf = CFrame.new(pos)
	if rot then
		cf = cf * CFrame.Angles(math.rad(rot.X), math.rad(rot.Y), math.rad(rot.Z))
	end
	return cf
end

-- The long axis of a box, as a world-space (weapon-local) direction.
local function longAxis(entry: any, cf: CFrame): Vector3
	local s = entry.size or Vector3.new(1, 1, 1)
	if s.Y >= s.X and s.Y >= s.Z then return cf.UpVector end
	if s.Z >= s.X and s.Z >= s.Y then return -cf.LookVector end
	return cf.RightVector
end

local function findEntry(def: any, names: {string}): any?
	if not def or not def.model then return nil end
	for _, want in ipairs(names) do
		for _, entry in ipairs(def.model) do
			if entry.n == want then return entry end
		end
	end
	return nil
end

-- The POSITION always comes from the authored anchor when there is one.
--
-- Those anchors are not decoration: Animations.luau drives the support hand
-- between `left`, `magwell`, `charge` and `pouch` during a reload, and they were
-- tuned as a set. Replacing `left` with the centre of whatever part is called
-- Foregrip moves the hand 0.4 studs down the handguard on the MK18 alone and
-- desynchronises it from every animation that hands off to it.
--
-- Only the ORIENTATION is derived, because that is the part the viewmodel never
-- had to specify -- its hand is a sphere.
local function deriveOne(def: any, names: {string}, authored: CFrame?): CFrame?
	local axis = UP
	local entry = findEntry(def, names)
	if entry then
		local cf = partCFrame(entry)
		axis = longAxis(entry, cf)
		-- Point the axis at the top of the grip (the trigger end), never down.
		if axis:Dot(UP) < 0 then axis = -axis end
	end
	local origin = authored and authored.Position
		or (entry and partCFrame(entry).Position)
	if not origin then return nil end
	return frame(origin, axis, FORWARD)
end

local cache = setmetatable({}, {__mode = "k"})

-- Returns {right = CFrame, left = CFrame} in weapon-local space, or nil.
function WeaponGrips.forDef(def: any): any?
	if not def then return nil end
	local hit = cache[def]
	if hit ~= nil then return hit or nil end

	-- A weapon converted from a real model ships measured frames; trust those.
	if def.handGrips then
		cache[def] = def.handGrips
		return def.handGrips
	end

	local authored = def.grips or {}
	local out = {
		right = deriveOne(def, RIGHT_NAMES, authored.right),
		left = deriveOne(def, LEFT_NAMES, authored.left),
	}
	if not out.right or not out.left then
		cache[def] = false
		return nil
	end
	cache[def] = out
	return out
end

return WeaponGrips
