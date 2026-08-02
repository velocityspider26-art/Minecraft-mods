--!nonstrict
-- CustomSoldierBuilder -- creates BLACKSITE's own soldier model from mesh data.
--
-- The visible body never comes from a Roblox avatar bundle. The normal R15
-- character remains invisible underneath as the replicated movement / Motor6D /
-- hit-detection skeleton. When runtime mesh creation is permitted, the exact
-- custom GLB geometry is reconstructed as MeshParts. If Roblox blocks those APIs
-- in a published experience, a custom anatomical fallback is generated from the
-- same model bounds instead of exposing the default Roblox character.

local AssetService = game:GetService("AssetService")

local Data = require(script.Parent:WaitForChild("CustomSoldierMesh"))

local Builder = {}
local cachedTemplate: Model? = nil
local cachedSource = "none"
local warned = false

local function vec3(a: any): Vector3
	return Vector3.new(a[1] or 0, a[2] or 0, a[3] or 0)
end

local MATERIALS = {
	Fabric = Enum.Material.Fabric,
	Metal = Enum.Material.Metal,
	Glass = Enum.Material.Glass,
	SmoothPlastic = Enum.Material.SmoothPlastic,
}

local function configure(part: BasePart, name: string, entry: any, offset: CFrame)
	part.Name = name
	part.Anchored = true
	part.Massless = true
	part.CanCollide = false
	part.CanTouch = false
	part.CanQuery = false
	part.CastShadow = true
	part.Reflectance = 0
	part.Material = MATERIALS[entry.material] or Enum.Material.SmoothPlastic
	local c = entry.color or {70, 72, 66}
	part.Color = Color3.fromRGB(c[1] or 70, c[2] or 72, c[3] or 66)
	part.Transparency = entry.material == "Glass" and 0.34 or 0
	part:SetAttribute("SourcePart", entry.source)
	part:SetAttribute("VisualGroup", entry.group)
	part:SetAttribute("LocalOffset", offset)
	pcall(function() (part :: any).DoubleSided = false end)
end

local function createMeshPart(entry: any): BasePart?
	local vertices, triangles, normals = entry.v, entry.t, entry.n
	if not vertices or not triangles or #vertices < 9 or #triangles < 3 then return nil end
	local ok, result = pcall(function()
		local editable = AssetService:CreateEditableMesh()
		local vertexIds = table.create(#vertices // 3)
		for i = 1, #vertices - 2, 3 do
			vertexIds[#vertexIds + 1] = editable:AddVertex(Vector3.new(vertices[i], vertices[i + 1], vertices[i + 2]))
		end
		local normalIds = nil
		if normals and #normals >= #vertices then
			normalIds = table.create(#normals // 3)
			for i = 1, #normals - 2, 3 do
				normalIds[#normalIds + 1] = editable:AddNormal(Vector3.new(normals[i], normals[i + 1], normals[i + 2]))
			end
		end
		for i = 1, #triangles - 2, 3 do
			local ia, ib, ic = triangles[i], triangles[i + 1], triangles[i + 2]
			local a, b, c = vertexIds[ia], vertexIds[ib], vertexIds[ic]
			if a and b and c then
				local face = editable:AddTriangle(a, b, c)
				if normalIds and face then
					pcall(function()
						editable:SetFaceNormals(face, {normalIds[ia], normalIds[ib], normalIds[ic]})
					end)
				end
			end
		end
		return AssetService:CreateMeshPartAsync(Content.fromObject(editable))
	end)
	if not ok or not result then return nil end
	return result :: BasePart
end

local function buildDetailed(): Model?
	local model = Instance.new("Model")
	model.Name = "BlacksiteCustomSoldierRuntime"
	local built = 0
	for _, name in ipairs(Data.parts) do
		local entry = Data[name]
		if entry then
			local part = createMeshPart(entry)
			if not part then
				model:Destroy()
				return nil
			end
			configure(part, name, entry, CFrame.identity)
			part.CFrame = CFrame.identity
			part.Parent = model
			built += 1
		end
	end
	if built < 15 then model:Destroy(); return nil end
	model:SetAttribute("BlacksiteCustomBody", true)
	model:SetAttribute("BlacksiteCustomBodySource", "RuntimeMesh")
	return model
end

local function buildFallback(): Model
	local model = Instance.new("Model")
	model.Name = "BlacksiteCustomSoldierFallback"
	for _, name in ipairs(Data.parts) do
		local entry = Data[name]
		if entry then
			local part = Instance.new("Part")
			part.Shape = entry.fallbackShape == "Ball" and Enum.PartType.Ball or Enum.PartType.Block
			part.Size = vec3(entry.size)
			local offset = CFrame.new(vec3(entry.center))
			configure(part, name, entry, offset)
			part.CFrame = offset
			-- Smooth the fallback silhouette. It remains BLACKSITE geometry, not a
			-- visible Roblox avatar, even when the runtime mesh APIs are unavailable.
			part.TopSurface = Enum.SurfaceType.Smooth
			part.BottomSurface = Enum.SurfaceType.Smooth
			part.Parent = model
		end
	end
	model:SetAttribute("BlacksiteCustomBody", true)
	model:SetAttribute("BlacksiteCustomBodySource", "CustomPrimitiveFallback")
	return model
end

function Builder.getTemplate(): (Model, string)
	if cachedTemplate and cachedTemplate.Parent == nil then
		return cachedTemplate, cachedSource
	end
	local detailed = buildDetailed()
	if detailed then
		cachedTemplate = detailed
		cachedSource = "RuntimeCustomMesh"
	else
		cachedTemplate = buildFallback()
		cachedSource = "CustomPrimitiveFallback"
		if not warned then
			warn("[BLACKSITE] Custom soldier mesh API unavailable. Using BLACKSITE's custom anatomical fallback. For the full model in a published experience, enable Experience Settings > Security > Allow Mesh & Image APIs, or import the included GLB once.")
			warned = true
		end
	end
	return cachedTemplate, cachedSource
end

function Builder.clearCache()
	if cachedTemplate then cachedTemplate:Destroy() end
	cachedTemplate = nil
	cachedSource = "none"
end

return Builder
