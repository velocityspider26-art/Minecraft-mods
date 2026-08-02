--!nonstrict
-- Viewmodel -- builds the first-person rig and draws it every frame.
--
-- The rig has three moving systems:
--
--   1. STATIC PARTS. Bolted to the weapon root, drawn at rootCF * offset.
--
--   2. BONES. Parts tagged with `bone` in Weapons.luau get an extra offset
--      from the animation system, in weapon-local space. This is how the
--      slide reciprocates, the magazine falls out, and the pump travels.
--
--   3. ARMS. Two-bone IK. The animation system supplies a hand target in
--      weapon-local space; the shoulder is a fixed point in camera space;
--      this module solves the elbow and draws upper arm, forearm and hand.
--      That is what makes the support hand *stick to the handguard* instead
--      of floating near it, and it is the single thing that reads as "fluid".
--
-- The arms are deliberately not anatomically proportioned. The shoulders sit
-- behind and below the camera where nothing can see them, and the bones are
-- long enough that the visible part -- forearm and hand entering frame from
-- the bottom -- lands where a real one would. Trying to make a to-scale human
-- arm reach a to-scale rifle handguard from an eye-mounted camera does not
-- work, and every FPS in existence cheats it the same way.
--
-- Everything lives under workspace.CurrentCamera, so none of it is raycastable
-- and none of it replicates.

local Cfg = require(script.Parent:WaitForChild("GameConfig"))
local Weapons = require(script.Parent:WaitForChild("Weapons"))
local VMC = Cfg.ARMS

local VM = {}

export type Pose = {
	camera: CFrame, -- tiny action-driven head/upper-body reaction
	screen: CFrame, -- whole-action framing; keeps hands and magwell on screen
	weapon: CFrame, -- additive on the weapon root
	bones: { [string]: CFrame }, -- weapon-local offsets, by bone name
	left: CFrame, -- support hand target, weapon-local
	right: CFrame, -- firing hand target, weapon-local
	leftBody: CFrame?, -- optional support-hand target in shoulder/body space
	leftBodyWeight: number?, -- blend from weapon target to body target
	leftAttachBone: string?, -- exact magazine bone ridden during grasp/contact
	leftAttachWeight: number?,
	leftAttachOffset: CFrame?,
	leftHandPose: string?,
	rightHandPose: string?,
	leftPole: Vector3?,
	rightPole: Vector3?,
	leftShoulder: Vector3?,
	rightShoulder: Vector3?,
	magHidden: boolean,
	spareMagHidden: boolean,
	hideRear: boolean?,
	cameraSafeScale: number?,
	flashRoll: number?, -- re-rolled per shot so no two flashes match
}

local GLOVE = Color3.fromRGB(27, 30, 31)
local SLEEVE = Color3.fromRGB(52, 58, 53)

local function mkPart(name: string, size: Vector3, color: Color3, mat: Enum.Material, parent: Instance): BasePart
	local p = Instance.new("Part")
	p.Name = name
	p.Size = size
	p.Color = color
	p.Material = mat
	p.Anchored = true
	p.CanCollide = false
	p.CanQuery = false
	p.CanTouch = false
	p.CastShadow = false
	p.TopSurface = Enum.SurfaceType.Smooth
	p.BottomSurface = Enum.SurfaceType.Smooth
	p.Parent = parent
	return p
end

-- Applies a model entry's `shape` and `transparency`, returning any extra
-- rotation the shape needs baked into its offset.
--
-- Roblox's Cylinder part has its circular axis on X, but every offset in
-- Weapons.luau is authored Z-forward. Rather than make every barrel in the
-- data carry a 90 degree yaw and break muzzleOffset's size.Z arithmetic, the
-- swap happens here: size is transposed and the rotation is returned to be
-- composed into the part's offset.
local function applyShape(p: BasePart, mp: any): CFrame
	if mp.transparency then
		p.Transparency = mp.transparency
	end
	if mp.shape == "cylinder" then
		p.Shape = Enum.PartType.Cylinder
		p.Size = Vector3.new(mp.size.Z, mp.size.Y, mp.size.X)
		return CFrame.Angles(0, math.rad(90), 0)
	elseif mp.shape == "ball" then
		p.Shape = Enum.PartType.Ball
	end
	return CFrame.identity
end

--------------------------------------------------------------------------------
-- Two-bone IK
--------------------------------------------------------------------------------

-- Returns the elbow position. If the target is further away than the arm can
-- reach the arm simply straightens -- the hand is drawn at the target either
-- way, so an out-of-range pose degrades into something that still looks
-- deliberate rather than snapping or erroring.
local function solveElbow(shoulder: Vector3, hand: Vector3, l1: number, l2: number, pole: Vector3): Vector3
	local toHand = hand - shoulder
	local dist = toHand.Magnitude
	if dist < 1e-3 then
		return shoulder + Vector3.new(0, -l1, 0)
	end
	local dir = toHand / dist

	local d = math.clamp(dist, math.abs(l1 - l2) + 0.02, l1 + l2 - 0.02)
	local cosA = math.clamp((l1 * l1 + d * d - l2 * l2) / (2 * l1 * d), -1, 1)
	local a = math.acos(cosA)

	-- A vector perpendicular to the shoulder->hand line, lying in the plane
	-- that contains the pole hint. This is what decides which way the elbow
	-- points; without it the solution is a whole circle of valid elbows.
	local side = dir:Cross(pole)
	if side.Magnitude < 1e-3 then
		side = dir:Cross(Vector3.new(0, 1, 0))
	end
	if side.Magnitude < 1e-3 then
		side = dir:Cross(Vector3.new(1, 0, 0))
	end
	local bend = side.Unit:Cross(dir).Unit
	if bend:Dot(pole) < 0 then
		bend = -bend
	end

	return shoulder + (dir * math.cos(a) + bend * math.sin(a)) * l1
end

local function drawBone(part: BasePart, from: Vector3, to: Vector3, length: number)
	local delta = to - from
	if delta.Magnitude < 1e-3 then
		part.CFrame = CFrame.new(from)
		return
	end
	if part.Shape == Enum.PartType.Cylinder then
		local x = delta.Unit
		local reference = math.abs(x:Dot(Vector3.yAxis)) > 0.94 and Vector3.zAxis or Vector3.yAxis
		local z = x:Cross(reference).Unit
		local y = z:Cross(x).Unit
		part.CFrame = CFrame.fromMatrix((from + to) * 0.5, x, y, z)
	else
		part.CFrame = CFrame.lookAt(from, to) * CFrame.new(0, 0, -length * 0.5)
	end
end

--------------------------------------------------------------------------------
-- Build
--------------------------------------------------------------------------------

export type Rig = {
	folder: Folder,
	def: any,
	static: { { part: BasePart, offset: CFrame } },
	bones: { [string]: { { part: BasePart, offset: CFrame } } },
	magParts: { BasePart },
	spareMagParts: { BasePart },
	actionBodyParts: { BasePart },
	normalBodyParts: { BasePart },
	arms: any,
	muzzle: Vector3,
	flash: BasePart,
	flashBloom: BasePart,
	flashPetals: { BasePart },
	flashEmitter: ParticleEmitter,
	blastSmoke: ParticleEmitter,
	smoke: ParticleEmitter,
	barrelSmoke: ParticleEmitter,
	flashLight: PointLight,
	fillAnchor: BasePart,
	fillLight: PointLight,
	laser: BasePart,
	laserDot: BasePart,
	thirdPerson: boolean,
	showInternalArms: boolean,
	visualSource: string,
}

local function buildArm(folder: Instance, side: string, upper: number, fore: number)
	local arm = {
		upper = mkPart(side .. "Upper", Vector3.new(upper, 0.30, 0.30), SLEEVE, Enum.Material.SmoothPlastic, folder),
		fore = mkPart(side .. "Fore", Vector3.new(fore, 0.27, 0.27), SLEEVE, Enum.Material.SmoothPlastic, folder),
		wrist = mkPart(side .. "Wrist", Vector3.new(0.25, 0.24, 0.22), SLEEVE, Enum.Material.SmoothPlastic, folder),
		hand = mkPart(side .. "Palm", Vector3.new(0.31, 0.18, 0.34), GLOVE, Enum.Material.SmoothPlastic, folder),
		thumb = mkPart(side .. "Thumb", Vector3.new(0.08, 0.09, 0.22), GLOVE, Enum.Material.SmoothPlastic, folder),
		fingers = {},
		upperLen = upper,
		foreLen = fore,
		sideSign = side == "Left" and -1 or 1,
	}
	for i = 1, 4 do
		arm.fingers[i] = mkPart(
			side .. "Finger" .. i,
			Vector3.new(0.055, 0.07, 0.25),
			GLOVE,
			Enum.Material.SmoothPlastic,
			folder
		)
	end
	arm.upper.Shape = Enum.PartType.Cylinder
	arm.fore.Shape = Enum.PartType.Cylinder
	arm.wrist.Shape = Enum.PartType.Ball
	arm.hand.Shape = Enum.PartType.Ball
	return arm
end

local HAND_POSES = {
	foregrip = { curl = 34, spread = 0.064, palmY = -0.01, fingerY = -0.115, fingerZ = -0.015, thumb = 42 },
	firing = { curl = 52, spread = 0.058, palmY = -0.015, fingerY = -0.105, fingerZ = -0.030, thumb = 34 },
	magazine = { curl = 67, spread = 0.057, palmY = -0.018, fingerY = -0.090, fingerZ = -0.040, thumb = 58 },
	dual_mag = { curl = 58, spread = 0.070, palmY = -0.012, fingerY = -0.095, fingerZ = -0.028, thumb = 70 },
	pinch = { curl = 22, spread = 0.072, palmY = 0.000, fingerY = -0.112, fingerZ = 0.000, thumb = 18 },
	controls = { curl = 28, spread = 0.068, palmY = -0.005, fingerY = -0.108, fingerZ = -0.005, thumb = 12 },
	shell = { curl = 72, spread = 0.054, palmY = -0.012, fingerY = -0.086, fingerZ = -0.035, thumb = 62 },
	hook = { curl = 45, spread = 0.082, palmY = -0.005, fingerY = -0.105, fingerZ = -0.015, thumb = 75 },
	open = { curl = 10, spread = 0.078, palmY = 0.005, fingerY = -0.128, fingerZ = 0.018, thumb = 18 },
}

local function drawHand(arm: any, target: CFrame, poseName: string?)
	local hp = HAND_POSES[poseName or "foregrip"] or HAND_POSES.foregrip
	arm.wrist.CFrame = target * CFrame.new(0, 0, 0.24)
	arm.hand.CFrame = target * CFrame.new(0, hp.palmY, 0.05)
	for i, finger in arm.fingers do
		local x = (i - 2.5) * hp.spread
		-- Index finger stays straighter on the firing hand. The remaining fingers
		-- close progressively, avoiding the four-identical-block claw silhouette.
		local stagger = (i - 1) * 4
		local curl = hp.curl + stagger
		if poseName == "firing" and i == 1 then
			curl = 14
		end
		finger.CFrame = target
			* CFrame.new(x, hp.fingerY, hp.fingerZ - (i - 1) * 0.006)
			* CFrame.Angles(math.rad(curl), 0, math.rad((i - 2.5) * 1.4))
	end
	arm.thumb.CFrame = target
		* CFrame.new(arm.sideSign * 0.180, -0.012, 0.035)
		* CFrame.Angles(math.rad(18), 0, arm.sideSign * math.rad(-hp.thumb))
end

--------------------------------------------------------------------------------
-- Marketplace / mesh override
--
-- Drop a Model into ReplicatedStorage/WeaponModels named after the weapon id
-- ("MK18", "M870", "P320") and it replaces the procedural parts entirely.
-- This is the path for a Toolbox or Blender gun and it deliberately does NOT
-- use InsertService, which does not work on this machine.
--
-- Requirements on the model:
--   * A PrimaryPart, or a part named "Root". Everything is positioned
--     relative to it, and it becomes the weapon's grip origin -- so put it
--     where the firing hand goes, not at the model's centre.
--   * Optional: a part named "Muzzle" marking where the flash sits.
--   * Optional: parts named Magazine / Slide / Bolt / Pump / ChargingHandle.
--     Those get bound to the animation bones automatically, so the magazine
--     still drops out and the slide still cycles.
--   * Optional: a "Scale" attribute on the Model if it was built world-sized.
--
-- The grip anchors stay in Weapons.luau -- a new model almost certainly needs
-- them re-tuned, and that is four CFrames.
--------------------------------------------------------------------------------
local BONE_BY_NAME = {
	Magazine = "mag",
	Mag = "mag",
	Bolt = "bolt",
	BoltCarrier = "bolt",
	Slide = "bolt",
	Pump = "pump",
	ForeEnd = "pump",
	ChargingHandle = "charge",
	Charge = "charge",
}

--------------------------------------------------------------------------------
-- RUNTIME MESH -- the real model, with no upload and no manual import.
--
-- I was wrong for several rounds about this. MeshPart.MeshId does require an
-- uploaded rbxassetid, but that is not the only way to get a mesh into a place:
-- AssetService:CreateEditableMesh() builds an empty mesh you can push vertices
-- and triangles into from code, and CreateMeshPartAsync() turns it into a real
-- MeshPart. So the weapon can simply ship as data -- see MK18Mesh.luau, which
-- tools/mk18_to_luau.py generates straight out of the .blend.
--
-- ONE CAVEAT, and the user has to do it once: in a PUBLISHED experience Roblox
-- gates these APIs behind Experience Settings -> Security -> "Allow Mesh &
-- Image APIs" (and ID verification). In Studio it works out of the box.
--
-- Every step is wrapped, and any failure falls through to the procedural box
-- model, so an ungated place or an older client still gets a working weapon
-- rather than an empty camera.
--------------------------------------------------------------------------------
local function liftViewmodelColor(color: Color3): Color3
	local h, s, v = color:ToHSV()
	-- Preserve a black rifle as black. Earlier builds forced every dark source
	-- colour to at least value 0.54, then illuminated it with a blue fill light;
	-- in daylight that produced the white weapon visible in the user's capture.
	-- Only rescue values that are genuinely crushed, and cap accidental bright
	-- imports so one untextured mesh cannot turn into a white silhouette.
	if v < 0.10 then
		v = 0.15 + v * 0.45
	elseif v > 0.48 then
		v = 0.34 + (v - 0.48) * 0.18
	end
	s = math.min(s, 0.34)
	return Color3.fromHSV(h, s, math.clamp(v, 0, 0.46))
end

local AssetService = game:GetService("AssetService")

local MESH_MODULES: { [string]: string } = {
	MK18 = "MK18Mesh",
}

-- Parts that exist only as markers and must never be drawn.
local MARKER_PARTS = { Root = true, Muzzle = true }

local function meshDataFor(def: any): any?
	local name = MESH_MODULES[def.id]
	if not name then
		return nil
	end
	local mod = script.Parent:FindFirstChild(name)
	if not mod or not mod:IsA("ModuleScript") then
		return nil
	end
	local ok, data = pcall(require, mod)
	return ok and data or nil
end

local function buildMeshPart(entry: any, maxTriangleZ: number?): BasePart?
	local v, t, n = entry.v, entry.t, entry.n
	if not v or not t or #v < 9 or #t < 3 then
		return nil
	end

	local ok, result = pcall(function()
		local em = AssetService:CreateEditableMesh()
		local ids = table.create(#v // 3)
		for i = 1, #v - 2, 3 do
			ids[#ids + 1] = em:AddVertex(Vector3.new(v[i], v[i + 1], v[i + 2]))
		end

		-- NORMALS ARE NOT OPTIONAL. A mesh with no normal data renders pure
		-- black -- the engine does not derive them from winding, and that is
		-- exactly what "the model is there but it's just black" was. These come
		-- straight out of Blender's autosmooth, one per vertex, and get attached
		-- per face corner.
		local nids = nil
		if n and #n >= #v then
			nids = table.create(#n // 3)
			for i = 1, #n - 2, 3 do
				nids[#nids + 1] = em:AddNormal(Vector3.new(n[i], n[i + 1], n[i + 2]))
			end
		end

		for i = 1, #t - 2, 3 do
			local i0, i1, i2 = t[i], t[i + 1], t[i + 2]
			local allowed = true
			if maxTriangleZ then
				local z0 = v[(i0 - 1) * 3 + 3]
				local z1 = v[(i1 - 1) * 3 + 3]
				local z2 = v[(i2 - 1) * 3 + 3]
				allowed = z0 <= maxTriangleZ and z1 <= maxTriangleZ and z2 <= maxTriangleZ
			end
			if allowed then
				local a, b, c = ids[i0], ids[i1], ids[i2]
				if a and b and c then
					local face = em:AddTriangle(a, b, c)
					if nids and face then
						-- Any older client without SetFaceNormals still gets the
						-- geometry; it just falls back to flat shading.
						pcall(function()
							em:SetFaceNormals(face, { nids[i0], nids[i1], nids[i2] })
						end)
					end
				end
			end
		end
		return AssetService:CreateMeshPartAsync(Content.fromObject(em))
	end)

	if not ok or not result then
		return nil
	end
	return result :: BasePart
end

-- Returns true if the whole weapon was built from mesh data.
local function buildFromMesh(rig: Rig, def: any, folder: Folder): boolean
	local data = meshDataFor(def)
	if not data or not data.parts then
		return false
	end

	local built: { BasePart } = {}
	for _, name in data.parts do
		local entry = data[name]
		if entry and not MARKER_PARTS[name] then
			local p = buildMeshPart(entry)
			if not p then
				-- Partial failure is worse than none: tear down and let the
				-- procedural model take over cleanly.
				for _, q in built do
					q:Destroy()
				end
				warn(("[BLACKSITE] EditableMesh build failed on '%s'. Falling back to the "
					.. "procedural model. If this is a published place, enable Experience "
					.. "Settings > Security > Allow Mesh & Image APIs."):format(name))
				return false
			end
			p.Name = name
			p.Anchored = true
			p.CanCollide = false
			p.CanQuery = false
			p.CanTouch = false
			p.CastShadow = false
			p.Material = Cfg.VIEWMODEL.MESH_MATERIAL
			p.Color = Cfg.VIEWMODEL.MESH_COLOR
			p.Reflectance = 0
			pcall(function()
				(p :: any).DoubleSided = Cfg.VIEWMODEL.MESH_DOUBLE_SIDED
			end)
			p.Parent = folder
			table.insert(built, p)

			-- The mesh is authored in weapon-local space, so its offset from the
			-- weapon root is identity -- no per-part transform to work out.
			local slot = { part = p, offset = CFrame.identity, normalBody = name == 'Body' }
			if name == 'Body' then
				table.insert(rig.normalBodyParts, p)
			end
			local bone = BONE_BY_NAME[name]
			if bone then
				local list = rig.bones[bone]
				if not list then
					list = {}
					rig.bones[bone] = list
				end
				table.insert(list, slot)
				if bone == "mag" then
					table.insert(rig.magParts, p)
				end
			else
				table.insert(rig.static, slot)
			end

			local lname = name:lower()
			if lname:find("glass") then
				p.Material = Enum.Material.Glass
				p.Transparency = lname:find("rear") and 0.95 or 0.86
				p.Color = Color3.fromRGB(60, 90, 78)
			elseif lname:find("dot") then
				p.Material = Enum.Material.Neon
				p.Color = Color3.fromRGB(255, 44, 30)
			elseif name == "Magazine" then
				p.Color = Cfg.VIEWMODEL.MESH_MAG_COLOR
			elseif name == "Bolt" then
				p.Color = Cfg.VIEWMODEL.MESH_BOLT_COLOR
			elseif name == "ChargingHandle" then
				p.Color = Cfg.VIEWMODEL.MESH_DETAIL_COLOR
			elseif name == "Body" then
				p.Color = Cfg.VIEWMODEL.MESH_COLOR
			else
				p.Color = Cfg.VIEWMODEL.MESH_DETAIL_COLOR
			end

			-- The generated MK18 body is one combined mesh, including receiver,
			-- buffer tube and stock. During reloads a normal first-person camera
			-- would be physically inside the stock. Build a second version that
			-- excludes the disconnected rear components (all lie past local Z .15).
			-- This preserves the receiver and handguard instead of hiding the gun.
			if name == 'Body' then
				local actionBody = buildMeshPart(entry, 0.15)
				if actionBody then
					actionBody.Name = 'ActionBody'
					actionBody.Anchored = true
					actionBody.CanCollide = false
					actionBody.CanQuery = false
					actionBody.CanTouch = false
					actionBody.CastShadow = false
					actionBody.Material = Cfg.VIEWMODEL.MESH_MATERIAL
					actionBody.Color = Cfg.VIEWMODEL.MESH_COLOR
					actionBody.Reflectance = 0
					actionBody.LocalTransparencyModifier = 1
					pcall(function() (actionBody :: any).DoubleSided = Cfg.VIEWMODEL.MESH_DOUBLE_SIDED end)
					actionBody.Parent = folder
					table.insert(built, actionBody)
					table.insert(rig.actionBodyParts, actionBody)
					table.insert(rig.static, {part=actionBody, offset=CFrame.identity, actionOnly=true})
				end
			end
		end
	end

	-- The flash lives at rig.muzzle, and that came from muzzleOffset() reading
	-- the PROCEDURAL model -- which is a different weapon at a different scale
	-- now. Left alone, the flash renders nearly a stud in front of the barrel
	-- and slightly above the bore, i.e. floating in mid-air.
	if #built > 0 and def.reach then
		rig.muzzle = Vector3.new(0, def.bore or 0.3, -def.reach)
	end

	return #built > 0
end

local function findOverride(def: any): Model?
	local ReplicatedStorage = game:GetService("ReplicatedStorage")
	local folder = ReplicatedStorage:FindFirstChild("WeaponModels")
	local m = folder and folder:FindFirstChild(def.id)
	if m and m:IsA("Model") then
		return m
	end
	return nil
end

local function buildFromOverride(rig: Rig, source: Model, folder: Folder): boolean
	local clone = source:Clone()
	local origin = clone.PrimaryPart or clone:FindFirstChild("Root")
	if not origin or not origin:IsA("BasePart") then
		warn(("[BLACKSITE] WeaponModels.%s has no PrimaryPart or Root part; ignoring it.")
			:format(source.Name))
		clone:Destroy()
		return false
	end

	local scale = source:GetAttribute("Scale") or 1
	local originCF = origin.CFrame

	for _, d in clone:GetDescendants() do
		if d:IsA("BasePart") then
			d.Anchored = true
			d.CanCollide = false
			d.CanQuery = false
			d.CanTouch = false
			d.CastShadow = false
			if scale ~= 1 then
				d.Size *= scale
			end

			-- An imported mesh arrives fully OPAQUE, so a lens group would be a
			-- solid disc across the sight line -- the exact bug the procedural
			-- optic had, where Material = Glass was set but Transparency was
			-- left at its default 0. Anything named *Glass* or *Lens* is glazed
			-- automatically, and the ocular goes nearly invisible because your
			-- eye is behind it rather than looking at it.
			local lname = d.Name:lower()
			if lname:find("glass") or lname:find("lens") then
				d.Material = Enum.Material.Glass
				d.Transparency = lname:find("rear") and 0.95 or 0.86
			elseif d.Material ~= Enum.Material.Neon then
				d.Color = liftViewmodelColor(d.Color)
			end

			-- Offset in weapon-local space, which is all the draw loop needs.
			local offset = originCF:ToObjectSpace(d.CFrame)
			if scale ~= 1 then
				offset = CFrame.new(offset.Position * scale) * (offset - offset.Position)
			end

			local entry = { part = d, offset = offset }
			local bone = BONE_BY_NAME[d.Name]
			if bone then
				local list = rig.bones[bone]
				if not list then
					list = {}
					rig.bones[bone] = list
				end
				table.insert(list, entry)
				if bone == "mag" then
					table.insert(rig.magParts, d)
				end
			else
				table.insert(rig.static, entry)
			end

			if d.Name == "Muzzle" then
				rig.muzzle = offset.Position
			end
		end
	end

	clone.Parent = folder
	return true
end

function VM.build(def: any, suppressed: boolean, parent: Instance, options: any?): Rig
	options = options or {}
	local thirdPerson = options.thirdPerson == true
	local showInternalArms = options.showInternalArms ~= false and not thirdPerson
	local folder = Instance.new("Folder")
	folder.Name = thirdPerson and "ThirdPersonWeapon" or "Viewmodel"
	folder.Parent = parent

	local rig: Rig = {
		folder = folder,
		def = def,
		static = {},
		bones = {},
		magParts = {},
		spareMagParts = {},
		actionBodyParts = {},
		normalBodyParts = {},
		arms = {
			L = buildArm(folder, "Left", VMC.SUPPORT_UPPER, VMC.SUPPORT_FORE),
			R = buildArm(folder, "Right", VMC.FIRING_UPPER, VMC.FIRING_FORE),
		},
		muzzle = Vector3.new(0, 0.3, -2),
		flash = nil :: any,
		flashBloom = nil :: any,
		flashPetals = {},
		flashEmitter = nil :: any,
		blastSmoke = nil :: any,
		smoke = nil :: any,
		barrelSmoke = nil :: any,
		flashLight = nil :: any,
		fillAnchor = nil :: any,
		fillLight = nil :: any,
		laser = nil :: any,
		laserDot = nil :: any,
		thirdPerson = thirdPerson,
		showInternalArms = showInternalArms,
		visualSource = "procedural",
	}

	rig.muzzle = Weapons.muzzleOffset(def, suppressed)

	-- Preference order: a Model you dropped in WeaponModels, then the runtime
	-- mesh, then the procedural boxes. A hand-placed Model wins because if you
	-- went to the trouble of importing one you meant it.
	local override = findOverride(def)
	local usedOverride = override ~= nil and buildFromOverride(rig, override :: Model, folder)
	if usedOverride then
		rig.visualSource = "override"
	elseif Cfg.VIEWMODEL.RUNTIME_MESH == true then
		usedOverride = buildFromMesh(rig, def, folder)
		if usedOverride then rig.visualSource = "runtime_mesh" end
	end

	if not usedOverride then
		-- Native Parts are serialized directly in the place. They cannot be
		-- blocked by published-experience EditableMesh permissions, so Studio
		-- and live servers now use the exact same weapon representation.
		rig.visualSource = "native_parts"
		for _, mp in def.model do
			if mp.optional == "suppressor" and not suppressed then
				continue
			end
			local rot = mp.rot or Vector3.zero
			local offset = CFrame.new(mp.pos) * CFrame.Angles(math.rad(rot.X), math.rad(rot.Y), math.rad(rot.Z))
			local authoredMaterial = mp.mat or Enum.Material.Metal
			local viewMaterial = authoredMaterial == Enum.Material.Metal
				and Cfg.VIEWMODEL.MESH_MATERIAL
				or authoredMaterial
			local p = mkPart(mp.n, mp.size, liftViewmodelColor(mp.color), viewMaterial, folder)
			offset = offset * applyShape(p, mp)

			local entry = { part = p, offset = offset }
			if mp.bone then
				local list = rig.bones[mp.bone]
				if not list then
					list = {}
					rig.bones[mp.bone] = list
				end
				table.insert(list, entry)
				if mp.bone == "mag" then
					table.insert(rig.magParts, p)
				end
			else
				table.insert(rig.static, entry)
			end
		end
	end

	----------------------------------------------------------------------------
	-- SECOND MAGAZINE FOR RETENTION RELOADS
	--
	-- A single animated magazine can only ever show "old disappears, new appears".
	-- Tactical L-index / beer-can exchanges require two magazines in the hand at
	-- the same time, so clone the actual magazine geometry into a separate bone.
	-- This works for procedural Parts, imported models and runtime MeshParts.
	----------------------------------------------------------------------------
	local magBone = rig.bones.mag
	if magBone and #magBone > 0 then
		rig.bones.spareMag = {}
		for i, entry in magBone do
			local ok, clone = pcall(function()
				return entry.part:Clone()
			end)
			if ok and clone and clone:IsA("BasePart") then
				clone.Name = "SpareMagazine" .. i
				clone.Anchored = true
				clone.CanCollide = false
				clone.CanQuery = false
				clone.CanTouch = false
				clone.CastShadow = false
				clone.Transparency = 1
				clone.Parent = folder
				table.insert(rig.bones.spareMag, { part = clone, offset = entry.offset })
				table.insert(rig.spareMagParts, clone)
			end
		end
	end

	----------------------------------------------------------------------------
	-- VIEWMODEL FILL LIGHT
	--
	-- The world grade intentionally keeps interiors dark, but a near-black rifle
	-- with no environment specular collapses into one silhouette. A tiny light
	-- travels with the receiver and has too little range to illuminate the room;
	-- it restores receiver/handguard separation without making the gun glow.
	----------------------------------------------------------------------------
	folder:SetAttribute("BlacksiteVisualSource", rig.visualSource)
	local fillAnchor = mkPart("ViewmodelFillAnchor", Vector3.one * 0.04, Color3.new(1, 1, 1), Enum.Material.SmoothPlastic, folder)
	fillAnchor.Transparency = 1
	local fillLight = Instance.new("PointLight")
	fillLight.Brightness = Cfg.VIEWMODEL.FILL_BRIGHTNESS
	fillLight.Range = Cfg.VIEWMODEL.FILL_RANGE
	fillLight.Color = Cfg.VIEWMODEL.FILL_COLOR
	fillLight.Shadows = false
	fillLight.Parent = fillAnchor
	rig.fillAnchor = fillAnchor
	rig.fillLight = fillLight

	----------------------------------------------------------------------------
	-- MUZZLE EFFECTS
	--
	-- No texture assets are available, so the flash is built from ellipsoids
	-- rather than billboard sprites. A tiny white core, a translucent bloom and
	-- six short irregular gas lobes read as burning powder; long rectangular
	-- neon rods read as a sci-fi star.
	----------------------------------------------------------------------------
	local MZ = Cfg.MUZZLE
	-- A tiny non-blooming white core and three narrow flame tongues. The previous
	-- neon sphere plus a 68-brightness light saturated BloomEffect into the giant
	-- white orb visible in the screenshots. These parts describe direction and
	-- pressure, not a glowing ball.
	local flash = mkPart(
		"FlashCore",
		Vector3.new(MZ.FLASH_CORE_THICK, MZ.FLASH_CORE_THICK, MZ.FLASH_CORE_LENGTH),
		MZ.FLASH_CORE_COLOR,
		Enum.Material.Neon,
		folder
	)
	flash.Shape = Enum.PartType.Ball
	flash.Transparency = 1
	rig.flash = flash

	-- Retained only for API compatibility with the firing code. It is a tiny warm
	-- gas kernel, not a full-screen bloom ball.
	local bloom = mkPart(
		"FlashBloom",
		Vector3.new(MZ.FLASH_GAS_THICK, MZ.FLASH_GAS_THICK, MZ.FLASH_GAS_LENGTH),
		MZ.FLASH_OUTER_COLOR,
		Enum.Material.Neon,
		folder
	)
	bloom.Shape = Enum.PartType.Ball
	bloom.Transparency = 1
	rig.flashBloom = bloom

	rig.flashPetals = {}
	for i = 1, MZ.FLASH_LOBES do
		local p = mkPart(
			"FlashTongue" .. i,
			Vector3.new(MZ.FLASH_LOBE_THICK, MZ.FLASH_LOBE_THICK, MZ.FLASH_LOBE_MIN),
			MZ.FLASH_OUTER_COLOR,
			Enum.Material.Neon,
			folder
		)
		p.Shape = Enum.PartType.Ball
		p.Transparency = 1
		table.insert(rig.flashPetals, p)
	end

	local pl = Instance.new("PointLight")
	pl.Brightness = MZ.FLASH_LIGHT
	pl.Range = MZ.FLASH_LIGHT_RANGE
	pl.Color = MZ.FLASH_CORE_COLOR
	pl.Shadows = false
	pl.Enabled = false
	pl.Parent = flash
	rig.flashLight = pl

	-- Camera-facing powder flame. The physical lobes give the flash volume from
	-- the side; this two-particle sprite makes the same 40 ms event readable when
	-- the bore points almost directly away from the eye, where thin 3D parts can
	-- disappear edge-on. It is compact and never emits a full-screen billboard.
	local flashEmitter = Instance.new("ParticleEmitter")
	flashEmitter.Name = "PowderFlash"
	flashEmitter.Enabled = true
	flashEmitter.Rate = 0
	flashEmitter.Texture = MZ.FLASH_TEXTURE
	flashEmitter.EmissionDirection = Enum.NormalId.Front
	flashEmitter.Orientation = Enum.ParticleOrientation.FacingCamera
	flashEmitter.Lifetime = NumberRange.new(0.028, 0.046)
	flashEmitter.Speed = NumberRange.new(1.0, 2.2)
	flashEmitter.SpreadAngle = Vector2.new(9, 9)
	flashEmitter.Size = NumberSequence.new({
		NumberSequenceKeypoint.new(0, 0.22),
		NumberSequenceKeypoint.new(0.20, 0.17),
		NumberSequenceKeypoint.new(1, 0.015),
	})
	flashEmitter.Transparency = NumberSequence.new({
		NumberSequenceKeypoint.new(0, 0.02),
		NumberSequenceKeypoint.new(0.28, 0.18),
		NumberSequenceKeypoint.new(1, 1),
	})
	flashEmitter.Color = ColorSequence.new(MZ.FLASH_CORE_COLOR, MZ.FLASH_OUTER_COLOR)
	flashEmitter.LightEmission = 1
	flashEmitter.LightInfluence = 0
	flashEmitter.Drag = 8
	flashEmitter.ZOffset = 0.02
	flashEmitter.Parent = flash
	rig.flashEmitter = flashEmitter

	local blastSmoke = Instance.new("ParticleEmitter")
	blastSmoke.Name = "MuzzlePressurePuff"
	blastSmoke.Enabled = true
	blastSmoke.Rate = 0
	blastSmoke.Texture = MZ.SMOKE_TEXTURE
	blastSmoke.EmissionDirection = Enum.NormalId.Front
	blastSmoke.Lifetime = NumberRange.new(MZ.BLAST_SMOKE_LIFE_MIN, MZ.BLAST_SMOKE_LIFE_MAX)
	blastSmoke.Speed = NumberRange.new(MZ.BLAST_SMOKE_SPEED_MIN, MZ.BLAST_SMOKE_SPEED_MAX)
	blastSmoke.SpreadAngle = Vector2.new(7, 7)
	blastSmoke.Size = NumberSequence.new({
		NumberSequenceKeypoint.new(0, MZ.BLAST_SMOKE_SIZE),
		NumberSequenceKeypoint.new(0.18, MZ.BLAST_SMOKE_SIZE * 1.8),
		NumberSequenceKeypoint.new(1, MZ.BLAST_SMOKE_SIZE * MZ.BLAST_SMOKE_GROW),
	})
	blastSmoke.Transparency = NumberSequence.new({
		NumberSequenceKeypoint.new(0, 0.46),
		NumberSequenceKeypoint.new(0.16, 0.58),
		NumberSequenceKeypoint.new(1, 1),
	})
	blastSmoke.Color = ColorSequence.new(Color3.fromRGB(176, 174, 168), Color3.fromRGB(108, 108, 105))
	blastSmoke.LightEmission = 0
	blastSmoke.LightInfluence = 0.92
	blastSmoke.Drag = 5.8
	blastSmoke.Rotation = NumberRange.new(0, 360)
	blastSmoke.RotSpeed = NumberRange.new(-55, 55)
	blastSmoke.VelocityInheritance = 0.08
	blastSmoke.ZOffset = 0
	blastSmoke.Parent = flash
	rig.blastSmoke = blastSmoke

	local smoke = Instance.new("ParticleEmitter")
	smoke.Name = "MuzzleLingeringSmoke"
	smoke.Enabled = true
	smoke.Rate = 0
	smoke.Texture = MZ.SMOKE_TEXTURE
	smoke.EmissionDirection = Enum.NormalId.Front
	smoke.Lifetime = NumberRange.new(MZ.LINGER_SMOKE_LIFE_MIN, MZ.LINGER_SMOKE_LIFE_MAX)
	smoke.Speed = NumberRange.new(MZ.LINGER_SMOKE_SPEED_MIN, MZ.LINGER_SMOKE_SPEED_MAX)
	smoke.SpreadAngle = Vector2.new(12, 12)
	smoke.Size = NumberSequence.new({
		NumberSequenceKeypoint.new(0, MZ.LINGER_SMOKE_SIZE),
		NumberSequenceKeypoint.new(0.28, MZ.LINGER_SMOKE_SIZE * 1.7),
		NumberSequenceKeypoint.new(1, MZ.LINGER_SMOKE_SIZE * MZ.LINGER_SMOKE_GROW),
	})
	smoke.Transparency = NumberSequence.new({
		NumberSequenceKeypoint.new(0, 0.63),
		NumberSequenceKeypoint.new(0.22, 0.68),
		NumberSequenceKeypoint.new(0.70, 0.86),
		NumberSequenceKeypoint.new(1, 1),
	})
	smoke.Color = ColorSequence.new(Color3.fromRGB(142, 143, 140), Color3.fromRGB(92, 93, 92))
	smoke.LightEmission = 0
	smoke.LightInfluence = 1
	smoke.Acceleration = Vector3.new(0, MZ.SMOKE_DRIFT, 0)
	smoke.Drag = 2.8
	smoke.Rotation = NumberRange.new(0, 360)
	smoke.RotSpeed = NumberRange.new(-22, 22)
	smoke.VelocityInheritance = 0.04
	smoke.ZOffset = 0
	smoke.Parent = flash
	rig.smoke = smoke

	local barrelSmoke = Instance.new("ParticleEmitter")
	barrelSmoke.Name = "HotBarrelSmoke"
	barrelSmoke.Enabled = true
	barrelSmoke.Rate = 0
	barrelSmoke.Texture = MZ.SMOKE_TEXTURE
	barrelSmoke.EmissionDirection = Enum.NormalId.Top
	barrelSmoke.Lifetime = NumberRange.new(MZ.HEAT_SMOKE_LIFE_MIN, MZ.HEAT_SMOKE_LIFE_MAX)
	barrelSmoke.Speed = NumberRange.new(MZ.HEAT_SMOKE_SPEED_MIN, MZ.HEAT_SMOKE_SPEED_MAX)
	barrelSmoke.SpreadAngle = Vector2.new(15, 15)
	barrelSmoke.Size = NumberSequence.new({
		NumberSequenceKeypoint.new(0, MZ.HEAT_SMOKE_SIZE),
		NumberSequenceKeypoint.new(1, MZ.HEAT_SMOKE_SIZE * MZ.HEAT_SMOKE_GROW),
	})
	barrelSmoke.Transparency = NumberSequence.new({
		NumberSequenceKeypoint.new(0, 0.78),
		NumberSequenceKeypoint.new(0.36, 0.80),
		NumberSequenceKeypoint.new(1, 1),
	})
	barrelSmoke.Color = ColorSequence.new(Color3.fromRGB(132, 133, 130), Color3.fromRGB(91, 92, 91))
	barrelSmoke.LightInfluence = 1
	barrelSmoke.Acceleration = Vector3.new(0, MZ.SMOKE_DRIFT * 0.72, 0)
	barrelSmoke.Drag = 3.4
	barrelSmoke.Rotation = NumberRange.new(0, 360)
	barrelSmoke.RotSpeed = NumberRange.new(-18, 18)
	barrelSmoke.ZOffset = 0
	barrelSmoke.Parent = flash
	rig.barrelSmoke = barrelSmoke

	local beam = mkPart("Laser", Vector3.new(0.02, 0.02, 1), Cfg.ATMO.LASER_COLOR, Enum.Material.Neon, folder)
	beam.Transparency = 1
	rig.laser = beam

	local dot = mkPart("LaserDot", Vector3.one * 0.09, Cfg.ATMO.LASER_COLOR, Enum.Material.Neon, folder)
	dot.Shape = Enum.PartType.Ball
	dot.Transparency = 1
	rig.laserDot = dot

	if not showInternalArms then
		-- The actual character body owns the visible arms. Keep the internal
		-- IK limbs hidden while preserving their hand targets for the real-arm solver.
		for _, arm in pairs(rig.arms) do
			for _, child in ipairs({arm.upper, arm.fore, arm.wrist, arm.hand, arm.thumb}) do
				child.LocalTransparencyModifier = 1
			end
			for _, finger in ipairs(arm.fingers) do finger.LocalTransparencyModifier = 1 end
		end
		rig.fillLight.Enabled = false
	end

	return rig
end

function VM.destroy(rig: Rig?)
	if rig and rig.folder then
		rig.folder:Destroy()
	end
end

--------------------------------------------------------------------------------
-- Draw
--------------------------------------------------------------------------------

-- rootCF   where the weapon sits this frame, already including pose blending,
--          sway, bob and recoil.
-- chestCF  the SHOULDER PLANE -- the top of the torso, which is what the arms
--          actually hang from. This used to be the camera, which meant the
--          shoulders were welded to the eyeball: turn your head and your
--          shoulders turned with it, instantly and exactly, so the arms could
--          never lag and there was no body between the mouse and the weapon.
--          The chest lags the head through a dead-band spring (C.BODY), and
--          feeding it in here is what makes the arms swing.
local REAR_FURNITURE = {
	StockBody = true,
	StockToe = true,
	ButtPad = true,
	CheekRest = true,
	BufferTube = true,
	CastleNut = true,
}

local function rearFurniture(name: string): boolean
	if REAR_FURNITURE[name] then
		return true
	end
	local lower = name:lower()
	return lower:find("stock") ~= nil or lower:find("butt") ~= nil or lower:find("cheek") ~= nil
end

-- Action rotations happen about the shoulder pocket, not the pistol-grip/root.
-- Rotating an AR around the grip sweeps the stock through the eye. Rotating it
-- around the butt pad keeps the stock planted while the receiver/muzzle move.
local function actionAboutPocket(rig: Rig, actionCF: CFrame): CFrame
	local pivot = rig.def.actionPivot or rig.def.pocket or Vector3.zero
	local translation = CFrame.new(actionCF.Position)
	local rotation = actionCF - actionCF.Position
	return translation * CFrame.new(pivot) * rotation * CFrame.new(-pivot)
end

local function cameraSafeWeaponCF(rig: Rig, weaponCF: CFrame, pose: Pose): CFrame
	local cam = workspace.CurrentCamera
	if not cam then
		return weaponCF
	end
	local cameraBack = cam.CFrame.ZVector
	local maxNear = -math.huge
	local function test(part: BasePart, cf: CFrame)
		-- Rear furniture is intentionally allowed to live behind the eye and is
		-- faded independently. Including it here used to shove the entire rifle
		-- forward every frame, then a reload rotation threw the stock into view.
		if part.Transparency >= 1 or rearFurniture(part.Name) then
			return
		end
		local half = part.Size * 0.5
		local extent = math.abs(cf.RightVector:Dot(cameraBack)) * half.X
			+ math.abs(cf.UpVector:Dot(cameraBack)) * half.Y
			+ math.abs(cf.LookVector:Dot(cameraBack)) * half.Z
		local centre = cam.CFrame:PointToObjectSpace(cf.Position)
		maxNear = math.max(maxNear, centre.Z + extent)
	end
	for _, e in rig.static do
		local skip = (e.actionOnly and not pose.hideRear)
			or (e.normalBody and pose.hideRear and #rig.actionBodyParts > 0)
		if not skip then
			test(e.part, weaponCF * e.offset)
		end
	end
	for boneName, list in rig.bones do
		local boneCF = pose.bones[boneName] or CFrame.identity
		for _, e in list do
			test(e.part, weaponCF * boneCF * e.offset)
		end
	end
	local safeZ = -Cfg.VIEWMODEL.NEAR_CLIP_SAFE
	if maxNear > safeZ then
		local scale = math.clamp(pose.cameraSafeScale or 1, 0, 1)
		local push = math.clamp(maxNear - safeZ, 0, Cfg.VIEWMODEL.NEAR_CLIP_MAX_PUSH * scale)
		return CFrame.new(cam.CFrame.LookVector * push) * weaponCF
	end
	return weaponCF
end

local function applyRearFade(part: BasePart, worldCF: CFrame, forceHide: boolean?)
	if not rearFurniture(part.Name) then
		part.LocalTransparencyModifier = 0
		return
	end
	if forceHide then
		part.LocalTransparencyModifier = 1
		return
	end
	local cam = workspace.CurrentCamera
	if not cam then
		part.LocalTransparencyModifier = 0
		return
	end
	local localPos = cam.CFrame:PointToObjectSpace(worldCF.Position)
	-- Fully hidden at/behind the near plane, smoothly restored once the stock is
	-- safely forward. This is the standard FPS cheat: rear geometry that would
	-- occupy the player's skull is not rendered in first person.
	part.LocalTransparencyModifier = math.clamp((localPos.Z + 0.46) / 0.14, 0, 1)
end

function VM.update(rig: Rig, rootCF: CFrame, chestCF: CFrame, pose: Pose)
	-- Framing translation and rifle articulation are different spaces. Folding
	-- them into one pivot made screen-space movement rotate around the butt pad
	-- and exposed the stock. Frame first, then articulate around the virtual
	-- shoulder pocket.
	local screenCF = rig.thirdPerson and CFrame.identity or pose.screen
	local weaponCF = rootCF * screenCF * actionAboutPocket(rig, pose.weapon)
	if not rig.thirdPerson then
		weaponCF = cameraSafeWeaponCF(rig, weaponCF, pose)
	end
	if rig.fillAnchor then
		rig.fillAnchor.CFrame = weaponCF * CFrame.new(-0.08, 0.46, -0.20)
	end

	for _, e in rig.static do
		local partCF = weaponCF * e.offset
		e.part.CFrame = partCF
		if e.actionOnly then
			e.part.LocalTransparencyModifier = 1
		elseif rig.thirdPerson then
			e.part.LocalTransparencyModifier = 0
		elseif e.normalBody then
			applyRearFade(e.part, partCF, false)
		else
			applyRearFade(e.part, partCF, pose.hideRear)
		end
	end

	for boneName, list in rig.bones do
		local boneCF = pose.bones[boneName] or CFrame.identity
		for _, e in list do
			local partCF = weaponCF * boneCF * e.offset
			e.part.CFrame = partCF
			if rig.thirdPerson then
				e.part.LocalTransparencyModifier = 0
			else
				applyRearFade(e.part, partCF, pose.hideRear)
			end
		end
	end

	local magT = pose.magHidden and 1 or 0
	for _, p in rig.magParts do
		p.Transparency = magT
	end

	local spareT = pose.spareMagHidden and 1 or 0
	for _, p in rig.spareMagParts do
		p.Transparency = spareT
	end

	local muzzleCF = weaponCF * CFrame.new(rig.muzzle)
	rig.flash.CFrame = muzzleCF * CFrame.new(0, 0, -rig.flash.Size.Z * 0.38)
	rig.flashBloom.CFrame = muzzleCF * CFrame.new(0, 0, -rig.flashBloom.Size.Z * 0.34)
	-- Lobes leave in a shallow cone around the bore. Ellipsoids overlap at the
	-- core, giving an irregular flame volume instead of a perfectly symmetric star.
	local roll = pose.flashRoll or 0
	for i, p in rig.flashPetals do
		local a = roll + (i - 1) * (math.pi * 2 / math.max(1, #rig.flashPetals))
		local cone = Cfg.MUZZLE.FLASH_CONE
		local pitch = math.sin(a) * cone
		local yaw = math.cos(a) * cone
		p.CFrame = muzzleCF
			* CFrame.Angles(pitch, yaw, a * 0.10)
			* CFrame.new(0, 0, -p.Size.Z * 0.46)
	end

	----------------------------------------------------------------------------
	-- Arms
	----------------------------------------------------------------------------
	local handR = weaponCF * pose.right
	local handL = weaponCF * pose.left
	local bodyWeight = math.clamp(pose.leftBodyWeight or 0, 0, 1)
	if bodyWeight > 0 and pose.leftBody then
		local bodyHand = chestCF * pose.leftBody
		handL = handL:Lerp(bodyHand, bodyWeight)
	end

	-- While a magazine is in the support hand, the HAND is the master transform
	-- and the magazine is its child. The previous system did the reverse, forcing
	-- a filtered arm target to chase an immediate mechanical bone; that is the
	-- source of the residual visible slip during carrier work and two-mag swaps.
	local heldWeight = math.clamp(pose.heldMagWeight or pose.leftAttachWeight or 0, 0, 1)
	local heldBone = pose.heldMagBone or pose.leftAttachBone
	if heldWeight > 0.001 and heldBone and heldBone ~= "none" then
		local list = rig.bones[heldBone]
		if list and #list > 0 then
			local anchorOffset = list[1].offset
			local gripBase = rig.def.grips.magwell or pose.left
			local anchorToHand = anchorOffset:ToObjectSpace(gripBase)
			local attachOffset = pose.leftAttachOffset or CFrame.identity
			local heldAnchorCF = handL * attachOffset:Inverse() * anchorToHand:Inverse()
			for _, entry in list do
				local localFromAnchor = anchorOffset:ToObjectSpace(entry.offset)
				local authoredCF = weaponCF * (pose.bones[heldBone] or CFrame.identity) * entry.offset
				entry.part.CFrame = authoredCF:Lerp(heldAnchorCF * localFromAnchor, heldWeight)
			end
		end
	end

	local shoulderR = (chestCF * CFrame.new(VMC.SHOULDER_R + (pose.rightShoulder or Vector3.zero))).Position
	local shoulderL = (chestCF * CFrame.new(VMC.SHOULDER_L + (pose.leftShoulder or Vector3.zero))).Position
	local poleR = chestCF:VectorToWorldSpace(VMC.POLE_R + (pose.rightPole or Vector3.zero)).Unit
	local poleL = chestCF:VectorToWorldSpace(VMC.POLE_L + (pose.leftPole or Vector3.zero)).Unit

	local armR, armL = rig.arms.R, rig.arms.L

	local elbowR = solveElbow(shoulderR, handR.Position, armR.upperLen, armR.foreLen, poleR)
	drawBone(armR.upper, shoulderR, elbowR, armR.upperLen)
	drawBone(armR.fore, elbowR, handR.Position, armR.foreLen)
	drawHand(armR, handR, pose.rightHandPose)

	local elbowL = solveElbow(shoulderL, handL.Position, armL.upperLen, armL.foreLen, poleL)
	drawBone(armL.upper, shoulderL, elbowL, armL.upperLen)
	drawBone(armL.fore, elbowL, handL.Position, armL.foreLen)
	drawHand(armL, handL, pose.leftHandPose)

	return weaponCF, handL, handR
end

-- Minimal remote-shot visual. The full local firing path still owns heat and
-- recoil; this helper lets every client see another player's muzzle event.
function VM.pulseMuzzle(rig: Rig, suppressed: boolean?)
	if not rig or not rig.folder.Parent then return end
	local MZ = Cfg.MUZZLE
	local scale = suppressed and MZ.SUPPRESSED_FLASH or 1
	rig.flash.Transparency = suppressed and .84 or .08
	rig.flashBloom.Transparency = suppressed and .94 or .55
	for _, p in ipairs(rig.flashPetals) do p.Transparency = suppressed and .96 or .48 end
	rig.flashLight.Brightness = MZ.FLASH_LIGHT * scale
	rig.flashLight.Enabled = not suppressed
	if rig.flashEmitter then rig.flashEmitter:Emit(suppressed and 1 or 2) end
	if rig.blastSmoke then rig.blastSmoke:Emit(suppressed and 1 or 3) end
	if rig.smoke then rig.smoke:Emit(1) end
	task.delay(MZ.FLASH_CORE_TIME, function()
		if not rig.folder.Parent then return end
		rig.flash.Transparency = 1
		rig.flashBloom.Transparency = 1
		rig.flashLight.Enabled = false
	end)
	task.delay(MZ.FLASH_AFTER_TIME, function()
		if not rig.folder.Parent then return end
		for _, p in ipairs(rig.flashPetals) do p.Transparency = 1 end
	end)
end

VM.solveElbow = solveElbow

return VM
