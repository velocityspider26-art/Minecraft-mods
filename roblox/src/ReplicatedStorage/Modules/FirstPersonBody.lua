--!nonstrict
-- FirstPersonBody -- true first-person visibility controller for the player's
-- actual humanoid rig.
--
-- V25 built a second body from loose Parts under CurrentCamera. It could never
-- match the third-person character because it was a different skeleton with
-- different proportions and a synthetic gait. V26 renders the same R15 body in
-- both perspectives: the server's humanoid owns anatomy and locomotion; this
-- module only hides the head/arms and fades geometry near the camera.

local Body = {}

local HEAD_NAMES = {
	Head=true, Face=true, HelmetDome=true, HelmetFront=true, HelmetRail=true,
	EarProL=true, EarProR=true, NeckGaiter=true,
}
local ARM_TOKENS = {"Arm", "Hand", "Glove", "Deltoid", "Forearm", "Wrist", "Finger", "Knuckle"}
local LEG_TOKENS = {"Leg", "Foot", "Thigh", "Knee", "Calf", "Ankle", "Boot", "Holster"}
local TORSO_TOKENS = {"Torso", "Chest", "Carrier", "Cummerbund", "Belt", "Pouch", "IFAK", "Dump", "Buckle", "Abdomen", "Pelvis", "Strap"}

local function containsAny(name: string, tokens): boolean
	for _, token in ipairs(tokens) do
		if string.find(name, token, 1, true) then
			return true
		end
	end
	return false
end

local function classify(part: BasePart): string
	local name = part.Name
	if name == "HumanoidRootPart" or HEAD_NAMES[name] then
		return "hidden"
	end
	if containsAny(name, ARM_TOKENS) then
		return "hidden" -- the dedicated weapon IK arms occupy this space
	end
	if containsAny(name, LEG_TOKENS) then
		return "legs"
	end
	if containsAny(name, TORSO_TOKENS) then
		return "torso"
	end
	-- Unknown welded tactical gear is safer in the torso group than floating in
	-- front of the camera at full opacity.
	return "torso"
end

local function nearPlaneVisibility(part: BasePart, cameraCF: CFrame): number
	local localCF = cameraCF:ToObjectSpace(part.CFrame)
	local half = part.Size * 0.5
	local extent = math.abs(localCF.RightVector.Z) * half.X
		+ math.abs(localCF.UpVector.Z) * half.Y
		+ math.abs(localCF.LookVector.Z) * half.Z
	local nearest = -localCF.Position.Z - extent
	return math.clamp((nearest - 0.075) / 0.20, 0, 1)
end

local function track(rig, instance: Instance)
	if not instance:IsA("BasePart") then return end
	if instance.Name == "TacLight" then return end
	rig.parts[instance] = classify(instance)
end

function Body.build(_parent, character: Model)
	local rig = {
		character = character,
		parts = {},
		connections = {},
	}
	for _, d in character:GetDescendants() do
		track(rig, d)
	end
	table.insert(rig.connections, character.DescendantAdded:Connect(function(d)
		track(rig, d)
	end))
	table.insert(rig.connections, character.DescendantRemoving:Connect(function(d)
		if d:IsA("BasePart") then
			rig.parts[d] = nil
		end
	end))
	return rig
end

function Body.update(rig, _rootCF, cameraCF, _stance, _lean, _gaitPhase, _moveFrac, _sprinting)
	if not rig or not rig.character or not rig.character.Parent then return end
	local down = -cameraCF.LookVector.Y
	-- The real body is always present in world space. These curves only stop the
	-- chest from grazing the near plane while looking level; looking down reveals
	-- the same torso, carrier, legs and boots that other players see.
	local torsoVisibility = math.clamp((down + 0.005) / 0.26, 0, 1) ^ 0.82
	local legVisibility = math.clamp((down + 0.16) / 0.24, 0, 1) ^ 0.70

	for part, group in rig.parts do
		if part.Parent then
			local visibility = 0
			if group == "torso" then
				visibility = torsoVisibility
			elseif group == "legs" then
				visibility = legVisibility
			end
			visibility *= nearPlaneVisibility(part, cameraCF)
			part.LocalTransparencyModifier = 1 - math.clamp(visibility, 0, 1)
		end
	end
end

function Body.destroy(rig)
	if not rig then return end
	for _, connection in ipairs(rig.connections or {}) do
		connection:Disconnect()
	end
	for part in rig.parts or {} do
		if part.Parent then
			part.LocalTransparencyModifier = 0
		end
	end
end

return Body
