--!nonstrict
-- HumanBody -- configures only the invisible articulated gameplay skeleton.
--
-- V33 deliberately does NOT request or display any Roblox avatar bundle. The
-- visible soldier is BLACKSITE's own custom mesh, built by CustomSoldierBuilder
-- and driven from this hidden R15 Motor6D skeleton. The skeleton exists only for
-- movement, replication, IK, hit detection and the anatomical damage system.

local HumanBody = {}

function HumanBody.createDescription(): (HumanoidDescription, string)
	local d = Instance.new("HumanoidDescription")
	-- Keep a stable R15 skeleton with human-ish joint spacing. No body, clothing,
	-- face, accessory or bundle asset IDs are requested here.
	d.HeightScale = 1.00
	d.WidthScale = 0.86
	d.DepthScale = 0.88
	d.HeadScale = 0.94
	d.BodyTypeScale = 0
	d.ProportionScale = 0
	return d, "BlacksiteCustomSkeleton"
end

function HumanBody.configure(character: Model, humanoid: Humanoid, source: string)
	humanoid.BreakJointsOnDeath = false
	humanoid.DisplayDistanceType = Enum.HumanoidDisplayDistanceType.None
	humanoid.HealthDisplayType = Enum.HumanoidHealthDisplayType.AlwaysOff

	-- Never expose the default Roblox character. Every client renders the custom
	-- soldier shell instead. Transparency does not affect raycasts or Motor6Ds.
	for _, d in character:GetDescendants() do
		if d:IsA("BasePart") then
			d.Transparency = 1
			d.CastShadow = false
			d.Reflectance = 0
		elseif d:IsA("Decal") or d:IsA("Texture") then
			d.Transparency = 1
		elseif d:IsA("Accessory") or d:IsA("Shirt") or d:IsA("Pants") or d:IsA("ShirtGraphic") then
			d:Destroy()
		end
	end
	character:SetAttribute("BlacksiteBodyStyle", "CustomSoldierMesh")
	character:SetAttribute("BlacksiteBodySource", source)
end

function HumanBody.buildTacticalGear(character: Model)
	-- Gear is already part of the custom soldier model. Intentionally no-op.
	local old = character:FindFirstChild("TacticalCharacterGear")
	if old then old:Destroy() end
end

return HumanBody
