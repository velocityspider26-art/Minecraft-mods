--!nonstrict
-- OperatorShell -- the real human body, driven from the pose the game already
-- computes.
--
-- WHY IT IS DRIVEN AND NOT DRIVING
--
-- The obvious way to fit a human skeleton into this game is to make it the
-- source of the pose and let the R15 rig chase it. That was tried and it broke
-- the weapon, for a reason worth writing down so it is not tried again:
--
--   FPSClient places the camera and the weapon from SoldierVisual.getChestCF /
--   getHeadCF. Those have to return the pose *this* frame. Roblox does not
--   guarantee that Part.CFrame reflects a Motor6D.Transform written earlier in
--   the same render step, so the only trustworthy answer is SoldierVisual's own
--   forward evaluation of the joint chain (rig.poseWorld). Anything that makes
--   those getters wait a frame -- or report a part that a slaving pass has
--   moved -- moves the gun.
--
-- So the R15 chain stays exactly what it was: posed first, evaluated into
-- poseWorld, and read by the camera and the weapon. This module runs afterwards
-- and retargets that finished pose onto the operator's own skeleton.
--
-- EVERY PART HAS TWO WAYS TO BE PLACED, AND THE SECOND ONE CANNOT FAIL
--
-- Primary: the human skeleton, which gives real clavicles, a two-segment spine
-- and toes. Fallback: an offset from the R15 part the segment belongs to,
-- measured once at build, which is exactly how the old body was drawn and is
-- therefore known to work.
--
-- The fallback is not decoration. Three rounds were lost to a body that built
-- correctly, reported correctly and rendered nothing, because a fault anywhere
-- in the solve left every part sitting at the world origin -- present, correct,
-- and a hundred studs from the player. A part that cannot reach its bone now
-- falls back to its R15 anchor instead of going home to 0,0,0.

local HumanRig = require(script.Parent:WaitForChild("HumanRig"))
local HumanPose = require(script.Parent:WaitForChild("HumanPose"))
local HumanArms = require(script.Parent:WaitForChild("HumanArms"))
local WeaponGrips = require(script.Parent:WaitForChild("WeaponGrips"))
local Data = require(script.Parent:WaitForChild("SoldierRigData"))

local OperatorShell = {}

-- Human bone -> the pair of R15 parts whose separation gives its direction.
-- Absent from this table means "no R15 equivalent": clavicles, toes and the
-- hands keep whatever HumanPose or the arm IK put there.
local DIR = {
	Hips = {"LowerTorso", "UpperTorso"},
	Spine = {"LowerTorso", "UpperTorso"},
	Spine1 = {"UpperTorso", "Head"},
	Neck = {"UpperTorso", "Head"},
	LeftArm = {"LeftUpperArm", "LeftLowerArm"},
	LeftForeArm = {"LeftLowerArm", "LeftHand"},
	RightArm = {"RightUpperArm", "RightLowerArm"},
	RightForeArm = {"RightLowerArm", "RightHand"},
	LeftUpLeg = {"LeftUpperLeg", "LeftLowerLeg"},
	LeftLeg = {"LeftLowerLeg", "LeftFoot"},
	RightUpLeg = {"RightUpperLeg", "RightLowerLeg"},
	RightLeg = {"RightLowerLeg", "RightFoot"},
}

-- The head is the one bone a direction cannot describe: looking up and down
-- barely moves the neck joint, it rotates about it.
local ROT = {Head = "Head"}

-- Which bone each direction is measured to in the bind pose.
local CHILD = {
	Hips = "Spine", Spine = "Spine1", Spine1 = "Neck", Neck = "Head",
	LeftArm = "LeftForeArm", LeftForeArm = "LeftHand",
	RightArm = "RightForeArm", RightForeArm = "RightHand",
	LeftUpLeg = "LeftLeg", LeftLeg = "LeftFoot",
	RightUpLeg = "RightLeg", RightLeg = "RightFoot",
}

-- Which R15 part each bone rides on when the skeleton cannot be trusted.
local SOURCE = {
	Hips = "LowerTorso", Spine = "LowerTorso", Spine1 = "UpperTorso",
	Neck = "UpperTorso", Head = "Head",
	LeftShoulder = "UpperTorso", LeftArm = "LeftUpperArm",
	LeftForeArm = "LeftLowerArm", LeftHand = "LeftHand",
	RightShoulder = "UpperTorso", RightArm = "RightUpperArm",
	RightForeArm = "RightLowerArm", RightHand = "RightHand",
	LeftUpLeg = "LeftUpperLeg", LeftLeg = "LeftLowerLeg",
	LeftFoot = "LeftFoot", LeftToeBase = "LeftFoot",
	RightUpLeg = "RightUpperLeg", RightLeg = "RightLowerLeg",
	RightFoot = "RightFoot", RightToeBase = "RightFoot",
}

local GROUP = {
	Pelvis = "torso", Abdomen = "torso", Chest = "torso",
	Neck = "head", Head = "head",
	LeftClavicle = "torso", RightClavicle = "torso",
	LeftUpperArm = "arms", LeftForeArm = "arms", LeftHand = "arms",
	RightUpperArm = "arms", RightForeArm = "arms", RightHand = "arms",
	LeftThigh = "legs", LeftShin = "legs", LeftFoot = "legs",
	RightThigh = "legs", RightShin = "legs", RightFoot = "legs",
}

local HIP_Y = Data.BONES.Hips.bind[2]
local HEAD_Y = Data.BONES.Head.bind[2]

local LEG_CHAIN = {"LeftUpLeg", "LeftLeg", "LeftFoot", "LeftToeBase",
	"RightUpLeg", "RightLeg", "RightFoot", "RightToeBase"}
local SPINE_CHAIN = {"Spine", "Spine1", "Neck", "Head",
	"LeftShoulder", "LeftArm", "LeftForeArm", "LeftHand",
	"RightShoulder", "RightArm", "RightForeArm", "RightHand"}

local BINDDIR = nil
local function bindDirs()
	if BINDDIR then return BINDDIR end
	BINDDIR = {}
	for bone, child in CHILD do
		local a = HumanRig.bindOf(bone).Position
		local b = HumanRig.bindOf(child).Position
		local d = b - a
		if d.Magnitude > 1e-5 then BINDDIR[bone] = d.Unit end
	end
	return BINDDIR
end

local function groundOf(character: Model): (CFrame?, number)
	local hrp = character:FindFirstChild("HumanoidRootPart")
	if not hrp or not hrp:IsA("BasePart") then return nil, 0 end
	local hum = character:FindFirstChildOfClass("Humanoid")
	local drop = (hum and hum.HipHeight or 2) + hrp.Size.Y * 0.5
	return hrp.CFrame * CFrame.new(0, -drop, 0), drop
end

--------------------------------------------------------------------------------
-- BUILD
--------------------------------------------------------------------------------

-- Fit the operator to the invisible R15 rig. The rig is what the camera, the
-- weapon and the hit detection are all tuned against, so it is the thing that
-- must not move; the body is what gets sized to it.
--
-- Two factors, not one. R15 carries its hips at roughly 60% of standing height
-- and a human carries them at 52%, so a single scale that puts the feet on the
-- floor puts the head half a stud above the camera. The leg chain is fitted to
-- the rig's pelvis height and the spine chain to its pelvis-to-head span.
local function fitScales(character: Model): (any, number)
	local hum = character:FindFirstChildOfClass("Humanoid")
	local hrp = character:FindFirstChild("HumanoidRootPart")
	local head = character:FindFirstChild("Head")
	local lower = character:FindFirstChild("LowerTorso") or character:FindFirstChild("Torso")
	if not hrp or not hrp:IsA("BasePart") then return 1, 1 end

	local pelvisY = (hum and hum.HipHeight or 2) + hrp.Size.Y * 0.5
	local legs = math.clamp(pelvisY / HIP_Y, 0.75, 1.3)

	local spine = legs
	if head and head:IsA("BasePart") and lower and lower:IsA("BasePart") then
		local span = head.Position.Y - lower.Position.Y
		if span > 0.1 then
			spine = math.clamp(span / (HEAD_Y - HIP_Y), 0.75, 1.3)
		end
	end

	local out = {}
	for _, b in ipairs(LEG_CHAIN) do out[b] = legs end
	for _, b in ipairs(SPINE_CHAIN) do out[b] = spine end
	out.Hips = legs
	return out, legs
end

function OperatorShell.build(character: Model, holder: Instance): any?
	local models = game:GetService("ReplicatedStorage"):FindFirstChild("CharacterModels")
	local imported = models and (models:FindFirstChild("SwatOperator")
		or models:FindFirstChild("MilitaryCustomRig"))
	if imported and not imported:IsA("Model") then imported = nil end

	local scales, legScale = fitScales(character)
	local hr = HumanRig.new(holder, imported, scales)
	-- No geometry means no body. Say so and let the caller keep the old shell
	-- rather than parenting an empty model and showing the player nothing.
	if hr.mode ~= "skinned" and #hr.parts == 0 then
		HumanRig.destroy(hr)
		return nil
	end

	local shell = {
		rig = hr,
		mode = hr.mode,
		scale = legScale,
		hipY = hr.bones.Hips.bind.Position.Y,
		footY = hr.bones.LeftFoot.bind.Position.Y,
		character = character,
		motion = HumanPose.newState(),
		base = {},
		pieces = #hr.parts,
		posed = false,
	}

	-- Measure each piece's offset from the R15 part it belongs to, with the
	-- character in the pose it spawned in. This is the placement that cannot
	-- fail, and it is what puts the body on screen on frame one -- before any
	-- solve has run.
	local ground = groundOf(character)
	if ground then
		for _, p in ipairs(hr.parts) do
			local srcName = SOURCE[p.bone]
			local src = srcName and character:FindFirstChild(srcName)
			local bone = hr.bones[p.bone]
			if src and src:IsA("BasePart") and bone then
				p.source = srcName
				p.anchor = src.CFrame:Inverse() * (ground * bone.bind * p.offset)
				p.part.CFrame = src.CFrame * p.anchor
			end
		end
	end

	return shell
end

function OperatorShell.destroy(shell: any)
	if shell and shell.rig then HumanRig.destroy(shell.rig) end
end

--------------------------------------------------------------------------------
-- RETARGET
--------------------------------------------------------------------------------

-- Where the operator stands: hips on the R15 pelvis, yaw from the root part.
-- Taking yaw alone matters -- the physics root pitches and rolls as it slides
-- over geometry and inheriting that makes the whole soldier wobble -- while
-- taking the POSITION from LowerTorso is what carries bob, crouch drop and lean
-- through, because those live in the root joint the R15 pose already applied.
local function anchor(character: Model, pw: any, hipY: number): CFrame?
	local hrp = character:FindFirstChild("HumanoidRootPart")
	if not hrp or not hrp:IsA("BasePart") then return nil end
	local pelvis = pw and pw.LowerTorso
	local look = hrp.CFrame.LookVector
	local flat = Vector3.new(look.X, 0, look.Z)
	flat = flat.Magnitude > 1e-4 and flat.Unit or Vector3.new(0, 0, -1)
	local origin = pelvis and pelvis.Position or hrp.Position
	return CFrame.lookAt(origin, origin + flat) * CFrame.new(0, -hipY, 0)
end

local function walk(hr: any, root: CFrame, pw: any, base: any)
	local bones = hr.bones
	local dirs = bindDirs()
	local rootRot = root.Rotation
	for _, name in ipairs(hr.order) do
		local b = bones[name]
		if b then
			local parent = b.parent and bones[b.parent]
			local parentWorld = parent and parent.world or root
			local world = parentWorld * (base[name] or b.pose)

			local rotPart = ROT[name]
			local pair = DIR[name]
			if rotPart and pw[rotPart] then
				world = CFrame.new(world.Position) * pw[rotPart].Rotation * b.bind.Rotation
			elseif pair then
				local a, c = pw[pair[1]], pw[pair[2]]
				local bd = dirs[name]
				if a and c and bd then
					local want = c.Position - a.Position
					if want.Magnitude > 1e-4 then
						local rot = HumanArms.swing(rootRot * bd, want.Unit)
							* (rootRot * b.bind.Rotation)
						world = CFrame.new(world.Position) * rot.Rotation
					end
				end
			end

			b.world = world
			b.pose = parentWorld:Inverse() * world
			b.delta = b.offset:Inverse() * b.pose
		end
	end
	hr.root = root
end

-- The R15 rig and the operator do not have identical leg lengths, so a pose
-- retargeted joint-angle-for-joint-angle can leave the feet a few centimetres
-- under or over the floor. Measuring the error and lifting the whole skeleton by
-- it is exact for the standing case and harmless otherwise.
local function groundError(character: Model, hr: any, footY: number): number
	local hrp = character:FindFirstChild("HumanoidRootPart")
	if not hrp or not hrp:IsA("BasePart") then return 0 end
	local hum = character:FindFirstChildOfClass("Humanoid")
	if hum and hum.FloorMaterial == Enum.Material.Air then return 0 end
	local lf, rf = hr.bones.LeftFoot, hr.bones.RightFoot
	if not lf or not rf then return 0 end
	local floor = hrp.Position.Y - ((hum and hum.HipHeight or 2) + hrp.Size.Y * 0.5)
	local lowest = math.min(lf.world.Position.Y, rf.world.Position.Y)
	return math.clamp((floor + footY) - lowest, -0.4, 0.4)
end

function OperatorShell.pose(shell: any, rig: any, state: any)
	shell.posed = false
	local hr = shell.rig
	local pw = rig.poseWorld
	if not pw or not pw.LowerTorso then return end
	local root = anchor(rig.character, pw, shell.hipY)
	if not root then return end

	HumanRig.reset(hr)
	-- Share the gait clock with the R15 body so the toes roll on the same step
	-- the legs are taking.
	if rig.motion and rig.motion.phase then shell.motion.phase = rig.motion.phase end
	HumanPose.update(hr, shell.motion, state, state and state.dt)

	-- Snapshot the procedural locals: the ground pass re-runs the walk, and the
	-- walk overwrites pose as it goes.
	local base = shell.base
	for name, b in hr.bones do base[name] = b.pose end

	walk(hr, root, pw, base)
	local dy = groundError(rig.character, hr, shell.footY)
	if math.abs(dy) > 0.002 then
		walk(hr, root + Vector3.new(0, dy, 0), pw, base)
	end

	-- Only now is the skeleton trusted. A pose that threw before this line
	-- leaves `posed` false and every piece falls back to its R15 anchor, which
	-- is a slightly stiffer body rather than no body at all.
	shell.posed = true
end

--------------------------------------------------------------------------------
-- ARMS
--------------------------------------------------------------------------------

-- The retarget above already has the operator's arms following the R15 arms,
-- which SoldierVisual has solved onto the viewmodel's hand targets. That is as
-- good as the old body ever was. This is the part that does better: when the
-- weapon's own grip frames are available, the wrist is placed where the measured
-- palm frame and the measured grip frame coincide, so the fingers close along
-- the grip instead of through it.
function OperatorShell.arms(shell: any, rig: any, weaponCF: CFrame?, def: any?, pose: any?)
	if not shell.posed or not weaponCF then return end
	local grips = WeaponGrips.forDef(def)
	if not grips then return end
	local ads = (pose and pose.adsAlpha) or shell.motion.ads or 0

	HumanArms.holdWeapon(shell.rig, weaponCF, grips, Data.HANDS, {
		ads = ads,
		leftTrim = def and def.gripTrimLeft,
		rightTrim = def and def.gripTrimRight,
	})
end

--------------------------------------------------------------------------------
-- DRAW
--------------------------------------------------------------------------------

local function nearPlane(part: BasePart, cameraCF: CFrame): number
	local localCF = cameraCF:ToObjectSpace(part.CFrame)
	local half = part.Size * 0.5
	local extent = math.abs(localCF.RightVector.Z) * half.X
		+ math.abs(localCF.UpVector.Z) * half.Y
		+ math.abs(localCF.LookVector.Z) * half.Z
	return math.clamp(((-localCF.Position.Z - extent) - 0.055) / 0.17, 0, 1)
end

function OperatorShell.draw(shell: any, rig: any, cameraCF: CFrame, isLocal: boolean,
	torsoVis: number, legsVis: number)
	local hr = shell.rig
	local pw = rig and rig.poseWorld

	if hr.mode == "skinned" then
		if shell.posed then HumanRig.apply(hr) end
		if hr.model then
			for _, d in hr.model:GetDescendants() do
				if d:IsA("BasePart") then
					-- One mesh, so it cannot be split by body part. In first
					-- person the near plane is what removes the head: it is the
					-- only thing close enough to the camera to be culled by it.
					d.LocalTransparencyModifier = isLocal and (1 - nearPlane(d, cameraCF)) or 0
				end
			end
		end
		return
	end

	for _, p in ipairs(hr.parts) do
		if p.part.Parent then
			local bone = shell.posed and hr.bones[p.bone]
			if bone then
				p.part.CFrame = bone.world * p.offset
			elseif p.anchor and pw and pw[p.source] then
				p.part.CFrame = pw[p.source] * p.anchor
			end

			local group = GROUP[p.group] or "torso"
			local vis
			if not isLocal then
				vis = 1
			elseif group == "head" then
				vis = 0
			elseif group == "arms" then
				vis = 1
			elseif group == "legs" then
				vis = legsVis
			else
				vis = torsoVis
			end
			if isLocal and vis > 0 then vis *= nearPlane(p.part, cameraCF) end
			p.part.LocalTransparencyModifier = 1 - math.clamp(vis, 0, 1)
		end
	end
end

-- Where the body actually ended up, for the startup diagnostic. "It built" and
-- "you can see it" are different claims and only the second one matters.
function OperatorShell.where(shell: any): (Vector3?, number)
	local hr = shell and shell.rig
	if not hr then return nil, 0 end
	if hr.mode == "skinned" then
		local part = hr.anchorPart
		return part and part.Position or nil, part and 1 or 0
	end
	local shown = 0
	local sum, n = Vector3.zero, 0
	for _, p in ipairs(hr.parts) do
		if p.part.Parent then
			sum += p.part.Position
			n += 1
			if p.part.LocalTransparencyModifier < 0.95 then shown += 1 end
		end
	end
	return n > 0 and (sum / n) or nil, shown
end

return OperatorShell
