--!nonstrict
-- Ballistics -- shared ray solving so the client's prediction and the server's
-- authoritative resolve use exactly the same maths.
--
-- Rounds are hitscan. At blacksite ranges (<60 m) a 5.56 round's flight time is
-- under 60 ms and its drop is a couple of centimetres, so simulating travel
-- would cost a lot and change nothing you could feel. What *is* simulated is
-- damage falloff and wall penetration, because those you feel constantly.

local Cfg = require(script.Parent:WaitForChild("GameConfig"))
local B = Cfg.BALLISTICS

local Ballistics = {}

export type Hit = {
	instance: BasePart,
	position: Vector3,
	normal: Vector3,
	distance: number,
	damageScale: number, -- falloff * penetration losses, 0..1
	layers: number,
}

-- Cone deviation. Uses sqrt on the radius so hits are uniformly distributed
-- across the cone's disc instead of clumping in the middle.
function Ballistics.applySpread(dir: Vector3, halfAngle: number, rng: Random): Vector3
	if halfAngle <= 0 then
		return dir
	end
	local up = math.abs(dir.Y) > 0.99 and Vector3.new(1, 0, 0) or Vector3.new(0, 1, 0)
	local right = dir:Cross(up).Unit
	local trueUp = right:Cross(dir).Unit

	local theta = rng:NextNumber(0, math.pi * 2)
	local r = math.tan(halfAngle) * math.sqrt(rng:NextNumber())
	return (dir + right * (math.cos(theta) * r) + trueUp * (math.sin(theta) * r)).Unit
end

function Ballistics.falloff(distance: number): number
	if distance <= B.FALLOFF_START then
		return 1
	end
	local t = math.clamp((distance - B.FALLOFF_START) / (B.MAX_RANGE - B.FALLOFF_START), 0, 1)
	return 1 - (1 - B.FALLOFF_MIN) * t
end

-- Fires one round and returns every surface it hit, in order. The list may be
-- empty (clean miss) or hold several entries if the round punched through.
function Ballistics.fire(origin: Vector3, dir: Vector3, ignore: { Instance }, canPenetrate: boolean): { Hit }
	local params = RaycastParams.new()
	params.FilterType = Enum.RaycastFilterType.Exclude
	params.FilterDescendantsInstances = ignore
	params.IgnoreWater = true

	local hits: { Hit } = {}
	local pos = origin
	local travelled = 0 -- total path length from the muzzle
	local scale = 1
	local layers = 0
	local blocked = table.clone(ignore)

	while true do
		local remaining = B.MAX_RANGE - travelled
		if remaining <= 0 then
			break
		end

		params.FilterDescendantsInstances = blocked
		local res = workspace:Raycast(pos, dir * remaining, params)
		if not res then
			break
		end

		local part = res.Instance
		travelled += (res.Position - pos).Magnitude

		table.insert(hits, {
			instance = part,
			position = res.Position,
			normal = res.Normal,
			distance = travelled,
			damageScale = scale * Ballistics.falloff(travelled),
			layers = layers,
		})

		if not canPenetrate or layers >= B.PEN_MAX_LAYERS then
			break
		end

		-- Find the far side of this part by raycasting back through it.
		local exit = Ballistics.findExit(part, res.Position, dir)
		if not exit then
			break
		end
		local thickness = (exit - res.Position).Magnitude
		if thickness > B.PEN_MAX_THICKNESS then
			break
		end

		scale -= thickness * B.PEN_LOSS_PER_STUD
		if scale <= 0.12 then
			break
		end

		layers += 1
		table.insert(blocked, part)
		travelled += thickness
		pos = exit + dir * 0.05
	end

	return hits
end

-- Raycast backwards from beyond the part to find where the round leaves it.
function Ballistics.findExit(part: BasePart, entry: Vector3, dir: Vector3): Vector3?
	local params = RaycastParams.new()
	params.FilterType = Enum.RaycastFilterType.Include
	params.FilterDescendantsInstances = { part }

	local far = entry + dir * (B.PEN_MAX_THICKNESS + 1)
	local res = workspace:Raycast(far, -dir * (B.PEN_MAX_THICKNESS + 1), params)
	return res and res.Position or nil
end

-- Walks up from a hit part to the character that owns it, if any.
function Ballistics.characterFrom(part: BasePart): (Model?, Humanoid?)
	local node: Instance? = part
	while node and node ~= workspace do
		if node:IsA("Model") then
			local hum = node:FindFirstChildOfClass("Humanoid")
			if hum then
				return node, hum
			end
		end
		node = node.Parent
	end
	return nil, nil
end

return Ballistics
