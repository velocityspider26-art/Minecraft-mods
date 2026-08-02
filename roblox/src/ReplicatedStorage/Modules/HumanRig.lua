--!nonstrict
-- HumanRig -- the real human skeleton that replaces R15 for everything visible.
--
-- R15 is not a human skeleton. It has no clavicles, one waist joint instead of
-- a spine, and no toes, which is why an R15 character holding a rifle always
-- reads as a doll: the shoulders cannot roll into the gun, the back cannot
-- round over it, and the feet cannot roll through a step. This module drives
-- the actual bind pose of the SWAT operator rig instead -- 21 animated bones
-- including two clavicles, a two-segment spine and toes -- measured straight
-- out of the source FBX by roblox/tools/convert_soldier.py.
--
-- The invisible R15 rig stays exactly where it is. It is still what Roblox
-- moves, replicates and collides, and still what the anatomy system raycasts
-- against; SoldierVisual drives it to follow this skeleton so the hitboxes and
-- the visible body agree.
--
-- THREE WAYS THE BODY CAN BE DRAWN, best first:
--
--   "skinned"    the operator FBX was imported into Studio, so there is a real
--                skinned MeshPart with Bone instances. We write Bone.Transform
--                and Roblox does true vertex skinning -- no seams, and the
--                Sketchfab textures come along. See roblox/README.md.
--   "mesh"       no import: rebuild the geometry at run time from the embedded
--                vertex data as one rigid MeshPart per bone.
--   "primitive"  mesh APIs unavailable: tapered blocks sized from the same
--                measurements, so the silhouette is still this soldier.

local AssetService = game:GetService("AssetService")

local Data = require(script.Parent:WaitForChild("SoldierRigData"))

local HumanRig = {}

HumanRig.HEIGHT = Data.HEIGHT

local MATERIALS = {
	Fabric = Enum.Material.Fabric,
	Metal = Enum.Material.Metal,
	SmoothPlastic = Enum.Material.SmoothPlastic,
	Glass = Enum.Material.Glass,
}

local function cf(t: {number}): CFrame
	return CFrame.new(t[1], t[2], t[3], t[4], t[5], t[6], t[7], t[8], t[9], t[10], t[11], t[12])
end

-- Bind data is parsed once and shared by every rig on the client.
local BIND = nil
local function bindTable()
	if BIND then return BIND end
	BIND = {}
	for name, entry in Data.BONES do
		BIND[name] = {
			parent = entry.parent,
			bind = cf(entry.bind),
			offset = cf(entry.offset),
		}
	end
	return BIND
end

--------------------------------------------------------------------------------
-- GEOMETRY
--------------------------------------------------------------------------------

-- Vertex normals from the triangle list. Cheaper than shipping a normal per
-- vertex in the data module, and identical in result.
local function computeNormals(v: {number}, t: {number}): {number}
	local n = table.create(#v, 0)
	for i = 1, #t - 2, 3 do
		local a, b, c = (t[i] - 1) * 3, (t[i + 1] - 1) * 3, (t[i + 2] - 1) * 3
		local ax, ay, az = v[a + 1], v[a + 2], v[a + 3]
		local bx, by, bz = v[b + 1], v[b + 2], v[b + 3]
		local cx, cy, cz = v[c + 1], v[c + 2], v[c + 3]
		local ux, uy, uz = bx - ax, by - ay, bz - az
		local wx, wy, wz = cx - ax, cy - ay, cz - az
		local fx, fy, fz = uy * wz - uz * wy, uz * wx - ux * wz, ux * wy - uy * wx
		for _, base in ipairs({a, b, c}) do
			n[base + 1] = (n[base + 1] or 0) + fx
			n[base + 2] = (n[base + 2] or 0) + fy
			n[base + 3] = (n[base + 3] or 0) + fz
		end
	end
	for i = 1, #n - 2, 3 do
		local x, y, z = n[i] or 0, n[i + 1] or 0, n[i + 2] or 0
		local len = math.sqrt(x * x + y * y + z * z)
		if len > 1e-6 then
			n[i], n[i + 1], n[i + 2] = x / len, y / len, z / len
		else
			n[i], n[i + 1], n[i + 2] = 0, 1, 0
		end
	end
	return n
end

local function buildMeshPart(entry: any): BasePart?
	local v, t = entry.v, entry.t
	if not v or not t or #v < 9 or #t < 3 then return nil end
	local ok, result = pcall(function()
		local editable = AssetService:CreateEditableMesh()
		local normals = computeNormals(v, t)
		local vid = table.create(#v // 3)
		local nid = table.create(#v // 3)
		for i = 1, #v - 2, 3 do
			vid[#vid + 1] = editable:AddVertex(Vector3.new(v[i], v[i + 1], v[i + 2]))
			nid[#nid + 1] = editable:AddNormal(Vector3.new(normals[i], normals[i + 1], normals[i + 2]))
		end
		for i = 1, #t - 2, 3 do
			local a, b, c = vid[t[i]], vid[t[i + 1]], vid[t[i + 2]]
			if a and b and c then
				local face = editable:AddTriangle(a, b, c)
				if face then
					pcall(function()
						editable:SetFaceNormals(face, {nid[t[i]], nid[t[i + 1]], nid[t[i + 2]]})
					end)
				end
			end
		end
		return AssetService:CreateMeshPartAsync(Content.fromObject(editable))
	end)
	if not ok or not result then return nil end
	return result :: BasePart
end

local function configure(part: BasePart, name: string, entry: any)
	part.Name = name
	part.Anchored = true
	part.Massless = true
	part.CanCollide = false
	part.CanTouch = false
	part.CanQuery = false
	part.CastShadow = true
	part.Material = MATERIALS[entry.material] or Enum.Material.Fabric
	local c = entry.color or {40, 43, 41}
	part.Color = Color3.fromRGB(c[1], c[2], c[3])
	part:SetAttribute("HumanBone", entry.bone)
	pcall(function() (part :: any).DoubleSided = false end)
end

--------------------------------------------------------------------------------
-- VISUAL SHELLS
--------------------------------------------------------------------------------

local cachedTemplate: Model? = nil
local cachedMode = "none"
local warned = false

local function buildTemplate(): (Model, string)
	local model = Instance.new("Model")
	model.Name = "BlacksiteOperatorShell"
	local built, wanted = 0, 0
	local mode = "mesh"
	for _, name in ipairs(Data.SEGMENTS) do
		local entry = Data[name]
		if entry then
			wanted += 1
			local part = buildMeshPart(entry)
			if part then
				configure(part, name, entry)
				part.Parent = model
				built += 1
			end
		end
	end
	if built < wanted then
		-- Mesh APIs are off (or over budget). Fall back to blocks sized from the
		-- same per-bone measurements, so the proportions stay this soldier's.
		model:Destroy()
		model = Instance.new("Model")
		model.Name = "BlacksiteOperatorShell"
		mode = "primitive"
		for _, name in ipairs(Data.SEGMENTS) do
			local entry = Data[name]
			if entry then
				local part = Instance.new("Part")
				part.Size = Vector3.new(entry.size[1], entry.size[2], entry.size[3])
				part.TopSurface = Enum.SurfaceType.Smooth
				part.BottomSurface = Enum.SurfaceType.Smooth
				configure(part, name, entry)
				part.Parent = model
			end
		end
		if not warned then
			warned = true
			warn("[BLACKSITE] Mesh APIs unavailable -- operator drawn as sized blocks. " ..
				"Enable Experience Settings > Security > Allow Mesh & Image APIs, or import " ..
				"roblox/assets/swat_operator.fbx once (see roblox/README.md) for the real body.")
		end
	end
	return model, mode
end

local function template(): (Model, string)
	if not cachedTemplate or cachedTemplate.Parent ~= nil then
		local m, mode = buildTemplate()
		cachedTemplate, cachedMode = m, mode
	end
	return cachedTemplate :: Model, cachedMode
end

function HumanRig.clearCache()
	if cachedTemplate then cachedTemplate:Destroy() end
	cachedTemplate, cachedMode = nil, "none"
end

-- An imported rig is any Model holding Bones whose names match the source
-- skeleton. Studio's 3D importer keeps the mixamorig: prefix; a hand-cleaned
-- rig may not, so both spellings are accepted.
local function findSkinnedBones(model: Instance): {[string]: Bone}
	local found = {}
	for _, d in model:GetDescendants() do
		if d:IsA("Bone") then
			local n = d.Name
			local short = string.match(n, "^mixamorig[:_]?(.+)$") or n
			if Data.BONES[short] then found[short] = d end
		end
	end
	return found
end

--------------------------------------------------------------------------------
-- RIG
--------------------------------------------------------------------------------

-- `source` is an optional imported Model (ReplicatedStorage.CharacterModels.*).
function HumanRig.new(parent: Instance, source: Model?): any
	local bind = bindTable()
	local rig = {
		bones = {},
		order = Data.ORDER,
		parts = {},
		mode = "mesh",
		root = CFrame.identity,
		model = nil,
		skinned = nil,
	}
	for name, entry in bind do
		rig.bones[name] = {
			parent = entry.parent,
			bind = entry.bind,
			offset = entry.offset,
			pose = entry.offset,   -- local, parent-relative
			world = entry.bind,
			delta = CFrame.identity,
		}
	end

	if source then
		local model = source:Clone()
		model.Name = "BlacksiteOperator"
		local bones = findSkinnedBones(model)
		if next(bones) ~= nil then
			rig.mode = "skinned"
			rig.skinned = bones
			for _, d in model:GetDescendants() do
				if d:IsA("BasePart") then
					d.Anchored = true
					d.CanCollide = false
					d.CanTouch = false
					d.CanQuery = false
					d.CastShadow = true
				end
			end
			rig.model = model
			model.Parent = parent
			-- The whole skinned model rides one part; find something to drive.
			rig.anchorPart = model:FindFirstChildWhichIsA("BasePart", true)
			return rig
		end
		model:Destroy()
	end

	local tmpl, mode = template()
	local model = tmpl:Clone()
	model.Name = "BlacksiteOperator"
	rig.mode = mode
	for _, d in model:GetChildren() do
		if d:IsA("BasePart") then
			local boneName = d:GetAttribute("HumanBone")
			local entry = Data[d.Name]
			if boneName and entry then
				rig.parts[#rig.parts + 1] = {
					part = d,
					bone = boneName,
					offset = CFrame.new(entry.centre[1], entry.centre[2], entry.centre[3]),
					group = d.Name,
				}
			end
		end
	end
	rig.model = model
	model.Parent = parent
	return rig
end

function HumanRig.destroy(rig: any)
	if not rig then return end
	if rig.model then rig.model:Destroy() end
	rig.model = nil
	rig.parts = {}
end

--------------------------------------------------------------------------------
-- POSING
--------------------------------------------------------------------------------

-- Clear every bone back to its bind pose. Call once per frame before the
-- animation layers write into it.
function HumanRig.reset(rig: any)
	for _, b in rig.bones do
		b.pose = b.offset
		b.delta = CFrame.identity
	end
end

-- Rotate/translate a bone relative to its bind pose. Layers compose.
function HumanRig.set(rig: any, name: string, delta: CFrame)
	local b = rig.bones[name]
	if not b then return end
	b.delta = delta
	b.pose = b.offset * delta
end

function HumanRig.add(rig: any, name: string, delta: CFrame)
	local b = rig.bones[name]
	if not b then return end
	b.delta = b.delta * delta
	b.pose = b.offset * b.delta
end

function HumanRig.get(rig: any, name: string): CFrame
	local b = rig.bones[name]
	return b and b.world or CFrame.identity
end

-- Force a bone to a world CFrame, keeping the chain consistent. Used by the
-- arm IK, which solves in world space against the weapon's grip anchors.
function HumanRig.setWorld(rig: any, name: string, world: CFrame)
	local b = rig.bones[name]
	if not b then return end
	local parent = b.parent and rig.bones[b.parent]
	local base = parent and parent.world or rig.root
	b.world = world
	b.pose = base:Inverse() * world
	b.delta = b.offset:Inverse() * b.pose
end

-- Evaluate the hierarchy. `rootCF` is the rig origin: the ground under the
-- hips, yaw-aligned with the character.
function HumanRig.solve(rig: any, rootCF: CFrame)
	rig.root = rootCF
	local bones = rig.bones
	for _, name in ipairs(rig.order) do
		local b = bones[name]
		if b then
			local parent = b.parent and bones[b.parent]
			b.world = (parent and parent.world or rootCF) * b.pose
		end
	end
end

-- Push the solved pose onto whatever is being drawn.
function HumanRig.apply(rig: any)
	if rig.mode == "skinned" then
		if rig.anchorPart then
			rig.anchorPart.CFrame = rig.root
		end
		for name, bone in rig.skinned do
			local b = rig.bones[name]
			if b then bone.Transform = b.delta end
		end
		return
	end
	for _, p in ipairs(rig.parts) do
		local b = rig.bones[p.bone]
		if b and p.part.Parent then
			p.part.CFrame = b.world * p.offset
		end
	end
end

function HumanRig.setTransparency(rig: any, fn: (string, BasePart) -> number)
	if rig.mode == "skinned" then
		if rig.model then
			for _, d in rig.model:GetDescendants() do
				if d:IsA("BasePart") then
					d.LocalTransparencyModifier = fn("body", d)
				end
			end
		end
		return
	end
	for _, p in ipairs(rig.parts) do
		if p.part.Parent then
			p.part.LocalTransparencyModifier = fn(p.bone, p.part)
		end
	end
end

--------------------------------------------------------------------------------
-- MEASUREMENTS -- taken from the bind pose so the IK uses this body's real
-- bone lengths instead of guessed ones.
--------------------------------------------------------------------------------

local LEN = nil
function HumanRig.lengths(): any
	if LEN then return LEN end
	local bind = bindTable()
	local function d(a: string, b: string): number
		return (bind[a].bind.Position - bind[b].bind.Position).Magnitude
	end
	LEN = {
		upperArm = d("LeftArm", "LeftForeArm"),
		foreArm = d("LeftForeArm", "LeftHand"),
		clavicle = d("LeftShoulder", "LeftArm"),
		thigh = d("LeftUpLeg", "LeftLeg"),
		shin = d("LeftLeg", "LeftFoot"),
		foot = d("LeftFoot", "LeftToeBase"),
		hipHeight = bind.Hips.bind.Position.Y,
		headHeight = bind.Head.bind.Position.Y,
		chestHeight = bind.Spine1.bind.Position.Y,
		height = Data.HEIGHT,
	}
	return LEN
end

function HumanRig.bindOf(name: string): CFrame
	local b = bindTable()[name]
	return b and b.bind or CFrame.identity
end

--------------------------------------------------------------------------------
-- ANATOMICAL SPACE
--
-- Mixamo bones point down their own +Y, and each one carries a different roll,
-- so CFrame.Angles(x, 0, 0) on a thigh does not mean "swing the leg forward" --
-- it means something different for every bone in the skeleton. Authoring
-- animation that way is unreadable and impossible to tune.
--
-- Everything in HumanPose is therefore written in CHARACTER space, where +X is
-- the soldier's right, +Y is up and +Z is behind them: pitch about X, yaw about
-- Y, roll about Z, for every bone alike. This converts such a rotation into the
-- bone-local delta that produces it.
--
--   world_new = translate(T) * rotate(R about the bone's own origin) * world_bind
--   delta     = Rb^-1 * translate(T) * R * Rb        (Rb = bind rotation)
--
-- The bind position cancels out of the conjugation, which is why only the
-- rotation part is needed here.
--------------------------------------------------------------------------------

local ROT, ROTINV = {}, {}
local function rotFrames(name: string): (CFrame, CFrame)
	local r = ROT[name]
	if not r then
		local b = bindTable()[name]
		r = b and b.bind.Rotation or CFrame.identity
		ROT[name] = r
		ROTINV[name] = r:Inverse()
	end
	return r, ROTINV[name]
end

function HumanRig.anatomical(name: string, rotation: CFrame?, translation: Vector3?): CFrame
	local r, rinv = rotFrames(name)
	local d = rotation or CFrame.identity
	if translation then d = CFrame.new(translation) * d end
	return rinv * d * r
end

-- Same conversion for a bone that must end up at an absolute world orientation
-- (the IK hands), rather than an offset from bind.
function HumanRig.worldRotationDelta(name: string, worldRot: CFrame): CFrame
	local r, rinv = rotFrames(name)
	return rinv * worldRot.Rotation * r
end

return HumanRig
