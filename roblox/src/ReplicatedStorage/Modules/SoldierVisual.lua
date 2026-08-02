--!nonstrict
-- SoldierVisual -- one body for first person, third person and weapon posing.
--
-- V36 replaces the R15-driven capsule shell with the real human skeleton from
-- the SWAT operator model (HumanRig / HumanPose / HumanArms). The public API is
-- unchanged, so FPSClient and CharacterVisuals keep calling exactly what they
-- called before.
--
-- WHAT DRIVES WHAT
--
--   HumanRig      the visible body. 21 real bones: clavicles, two spine
--                 segments, toes. This is what is drawn and what the weapon is
--                 aligned against.
--   R15           still present, still invisible, still what Roblox moves,
--                 replicates and collides. It is now SLAVED to the human
--                 skeleton (see syncR15) so that the anatomy raycasts in
--                 GameServer hit where the soldier visibly is -- crouch behind
--                 cover and the hitboxes crouch too, which they did not do
--                 when the two skeletons were posed independently.
--
-- The R15 rig is never the source of a pose any more. It is a shadow.

local ReplicatedStorage = game:GetService("ReplicatedStorage")

local Modules = script.Parent
local HumanRig = require(Modules:WaitForChild("HumanRig"))
local HumanPose = require(Modules:WaitForChild("HumanPose"))
local HumanArms = require(Modules:WaitForChild("HumanArms"))
local RigData = require(Modules:WaitForChild("SoldierRigData"))
local WeaponGrips = require(Modules:WaitForChild("WeaponGrips"))
local Cfg = require(Modules:WaitForChild("GameConfig"))

local SoldierVisual = {}

-- human bone -> the R15 part it drags along, and that part's distal joint
local R15_MAP = {
	{bone = "Hips", child = "Spine", part = "LowerTorso", motor = "Root"},
	{bone = "Spine1", child = "Neck", part = "UpperTorso", motor = "Waist"},
	{bone = "Head", child = "Head", part = "Head", motor = "Neck"},
	{bone = "LeftArm", child = "LeftForeArm", part = "LeftUpperArm", motor = "LeftShoulder"},
	{bone = "LeftForeArm", child = "LeftHand", part = "LeftLowerArm", motor = "LeftElbow"},
	{bone = "LeftHand", child = "LeftHand", part = "LeftHand", motor = "LeftWrist"},
	{bone = "RightArm", child = "RightForeArm", part = "RightUpperArm", motor = "RightShoulder"},
	{bone = "RightForeArm", child = "RightHand", part = "RightLowerArm", motor = "RightElbow"},
	{bone = "RightHand", child = "RightHand", part = "RightHand", motor = "RightWrist"},
	{bone = "LeftUpLeg", child = "LeftLeg", part = "LeftUpperLeg", motor = "LeftHip"},
	{bone = "LeftLeg", child = "LeftFoot", part = "LeftLowerLeg", motor = "LeftKnee"},
	{bone = "LeftFoot", child = "LeftToeBase", part = "LeftFoot", motor = "LeftAnkle"},
	{bone = "RightUpLeg", child = "RightLeg", part = "RightUpperLeg", motor = "RightHip"},
	{bone = "RightLeg", child = "RightFoot", part = "RightLowerLeg", motor = "RightKnee"},
	{bone = "RightFoot", child = "RightToeBase", part = "RightFoot", motor = "RightAnkle"},
}

local function motor(character: Model, name: string): Motor6D?
	local d = character:FindFirstChild(name, true)
	return d and d:IsA("Motor6D") and d or nil
end

local function findPart(character: Model, ...): BasePart?
	for _, name in ipairs({...}) do
		local p = character:FindFirstChild(name)
		if p and p:IsA("BasePart") then return p end
	end
	return nil
end

-- Roblox's default Animate script keeps applying walk/run tracks underneath,
-- which fights every joint this module writes.
local function disableDefaultAnimator(character: Model)
	local animate = character:FindFirstChild("Animate")
	if animate and animate:IsA("LocalScript") then animate.Disabled = true end
	local hum = character:FindFirstChildOfClass("Humanoid")
	local animator = hum and hum:FindFirstChildOfClass("Animator")
	if animator then
		for _, track in animator:GetPlayingAnimationTracks() do
			track:Stop(0.08)
		end
	end
end

--------------------------------------------------------------------------------

function SoldierVisual.build(character: Model): any
	disableDefaultAnimator(character)

	-- An imported, textured, skinned copy wins over the runtime-built one.
	local models = ReplicatedStorage:FindFirstChild("CharacterModels")
	local imported = models and (models:FindFirstChild("SwatOperator")
		or models:FindFirstChild("MilitaryCustomRig"))

	local holder = Instance.new("Model")
	holder.Name = "BlacksiteSoldierVisual"
	holder.Parent = character

	local rig = HumanRig.new(holder, imported and imported:IsA("Model") and imported or nil)
	rig.character = character
	rig.holder = holder
	rig.motion = HumanPose.newState()
	rig.hidden = {}
	rig.r15 = {}
	for _, m in ipairs(R15_MAP) do
		rig.r15[m.motor] = motor(character, m.motor)
			or motor(character, (string.gsub(m.motor, "(%l)(%u)", "%1 %2")))
	end

	-- Hide the R15 shell. It stays for movement, replication and raycasts.
	for _, d in character:GetDescendants() do
		if d:IsA("BasePart") and not d:IsDescendantOf(holder) then
			rig.hidden[d] = d.LocalTransparencyModifier
			d.LocalTransparencyModifier = 1
		end
	end

	character:SetAttribute("BlacksiteVisualBody", rig.mode)
	-- One line, on purpose. "The body looks wrong" is not diagnosable from the
	-- outside; which of the three draw paths was taken, and whether it actually
	-- produced geometry, answers it immediately.
	print(("[BLACKSITE] operator body: mode=%s pieces=%d bones=%d skinned=%s")
		:format(rig.mode, #rig.parts, #rig.order, tostring(rig.skinned ~= nil)))
	return rig
end

function SoldierVisual.destroy(rig: any)
	if not rig then return end
	for part, original in rig.hidden or {} do
		if part.Parent then part.LocalTransparencyModifier = original end
	end
	HumanRig.destroy(rig)
	if rig.holder then rig.holder:Destroy() end
	rig.holder = nil
end

--------------------------------------------------------------------------------
-- ROOT
--
-- The rig is authored standing on the floor under its own hips, so the root is
-- the character's ground point, yaw-only. Taking the yaw alone matters: the
-- physics root pitches and rolls as it slides over geometry, and inheriting
-- that makes the whole soldier wobble.
--------------------------------------------------------------------------------
local function rootCFrame(character: Model): CFrame?
	local hrp = findPart(character, "HumanoidRootPart")
	if not hrp then return nil end
	local hum = character:FindFirstChildOfClass("Humanoid")
	local look = hrp.CFrame.LookVector
	local flat = Vector3.new(look.X, 0, look.Z)
	if flat.Magnitude < 1e-4 then flat = Vector3.new(0, 0, -1) else flat = flat.Unit end
	local drop = (hum and hum.HipHeight or 2) + hrp.Size.Y * 0.5
	local ground = hrp.Position - Vector3.new(0, drop, 0)
	return CFrame.lookAt(ground, ground + flat)
end

--------------------------------------------------------------------------------
-- R15 SHADOW
--------------------------------------------------------------------------------
local function setMotorWorld(m: Motor6D?, desired: CFrame, part0CF: CFrame?)
	if not m or not m.Parent or not m.Part0 or not m.Part1 then return end
	local base = part0CF or m.Part0.CFrame
	m.Transform = m.C0:Inverse() * base:Inverse() * desired * m.C1
end

-- Lay each R15 part over the matching human bone. R15 limbs are centred on the
-- segment with +Y running back up toward the parent joint, so the part goes at
-- the midpoint of the bone and its child, not on the joint itself.
local function syncR15(rig: any)
	local character = rig.character
	if not character then return end
	for _, m in ipairs(R15_MAP) do
		local bone = rig.bones[m.bone]
		local child = rig.bones[m.child]
		local part = findPart(character, m.part)
		if bone and child and part then
			local a = bone.world.Position
			local b = child.world.Position
			local up = a - b
			local desired
			if up.Magnitude < 1e-3 then
				-- Terminal segment (hand, head): no child to point at, so keep
				-- the bone's own orientation.
				desired = bone.world
			else
				up = up.Unit
				local fwd = bone.world.LookVector
				local right = up:Cross(fwd)
				if right.Magnitude < 1e-3 then right = up:Cross(Vector3.zAxis) end
				if right.Magnitude < 1e-3 then right = up:Cross(Vector3.xAxis) end
				right = right.Unit
				desired = CFrame.fromMatrix((a + b) * 0.5, right, up)
			end
			setMotorWorld(rig.r15[m.motor], desired)
		end
	end
end

--------------------------------------------------------------------------------
-- POSE
--------------------------------------------------------------------------------

function SoldierVisual.poseBody(rig: any, state: any)
	if not rig or not rig.character or not rig.character.Parent then return end
	local root = rootCFrame(rig.character)
	if not root then return end
	state = state or {}

	HumanRig.reset(rig)
	HumanPose.update(rig, rig.motion, state, tonumber(state.dt) or rig.motion.dt)
	HumanRig.solve(rig, root)
	rig.solved = true
	rig.posedArms = false

	-- Lay the R15 shadow over the new pose NOW, not in updateBody. FPSClient
	-- reads getChestCF/getHeadCF between poseBody and updateBody to place the
	-- camera and the weapon; syncing afterwards would hand it last frame's
	-- skeleton and the whole viewmodel would trail the body by a frame.
	syncR15(rig)
end

function SoldierVisual.poseArms(rig: any, chestCF: CFrame, handL: CFrame, handR: CFrame,
	pose: any, weaponCF: CFrame?, def: any?)
	if not rig or not rig.bones then return end

	local H = Cfg.HUMAN_ANIM
	local ads = (pose and pose.adsAlpha) or (rig.motion and rig.motion.ads) or 0
	local grips = weaponCF and WeaponGrips.forDef(def)

	if grips then
		-- Both ends of the contact are measured frames, so the hands land ON
		-- the grips at the right roll rather than being aimed near them.
		HumanArms.holdWeapon(rig, weaponCF, grips, RigData.HANDS, {
			ads = ads,
			leftTrim = def and def.gripTrimLeft,
			rightTrim = def and def.gripTrimRight,
		})
	else
		-- Nothing to measure (no model list, or no weapon CFrame): fall back to
		-- the viewmodel's hand targets. Position is honoured exactly; the roll
		-- comes from the weapon's own axes, which is still better than the
		-- viewmodel convention, whose "hand" is a sphere.
		if handL then
			local palm = WeaponGrips.frame(handL.Position, handL.UpVector, -handL.LookVector)
			HumanArms.solveTo(rig, "Left", palm * RigData.HANDS.Left:Inverse(),
				H.POLE_SUPPORT:Lerp(H.POLE_SUPPORT_ADS, ads))
		end
		if handR then
			local palm = WeaponGrips.frame(handR.Position, handR.UpVector, -handR.LookVector)
			HumanArms.solveTo(rig, "Right", palm * RigData.HANDS.Right:Inverse(),
				H.POLE_FIRING:Lerp(H.POLE_FIRING_ADS, ads))
		end
	end
	rig.posedArms = true
end

--------------------------------------------------------------------------------
-- DRAW
--------------------------------------------------------------------------------

-- In first person the soldier's own head and chest would fill the screen, so
-- they fade out as the camera looks up and come back as it looks down at the
-- body -- which is what makes looking down and seeing your own legs work.
local function visibilityFor(bone: string, isLocal: boolean, torso: number, legs: number): number
	if not isLocal then return 1 end
	if bone == "Head" or bone == "Neck" then return 0 end
	if string.find(bone, "Arm") or string.find(bone, "Hand") or string.find(bone, "Shoulder") then
		return 1
	end
	if string.find(bone, "Leg") or string.find(bone, "Foot") or string.find(bone, "Toe") then
		return legs
	end
	return torso
end

function SoldierVisual.updateBody(rig: any, cameraCF: CFrame, isLocal: boolean)
	if not rig or not rig.holder or not rig.holder.Parent then return end
	HumanRig.apply(rig)

	-- Re-hide the R15 shell EVERY frame. LocalTransparencyModifier is not a
	-- setting, it is a per-frame value: Roblox's own first-person handling
	-- rewrites it on the local character, so hiding the shell once at build
	-- time lasts exactly until the next time the camera touches it, and then
	-- the old R15 body pops back through the new one.
	for part in rig.hidden do
		if part.Parent then part.LocalTransparencyModifier = 1 end
	end

	local down = -cameraCF.LookVector.Y
	local torso = math.clamp((down + 0.015) / 0.22, 0, 1) ^ 0.72
	local legs = math.clamp((down + 0.19) / 0.22, 0, 1) ^ 0.62
	HumanRig.setTransparency(rig, function(bone, part)
		local vis = visibilityFor(bone, isLocal, torso, legs)
		if isLocal and vis > 0 then
			-- Fade anything about to clip through the near plane.
			local localCF = cameraCF:ToObjectSpace(part.CFrame)
			local half = part.Size * 0.5
			local extent = math.abs(localCF.RightVector.Z) * half.X
				+ math.abs(localCF.UpVector.Z) * half.Y
				+ math.abs(localCF.LookVector.Z) * half.Z
			local nearest = -localCF.Position.Z - extent
			vis *= math.clamp((nearest - 0.055) / 0.17, 0, 1)
		end
		return 1 - math.clamp(vis, 0, 1)
	end)
end

--------------------------------------------------------------------------------
-- QUERIES
--------------------------------------------------------------------------------

-- These two are read by FPSClient to place the CAMERA and, through
-- CHEST_TO_SHOULDER, the weapon itself. They must keep returning what they
-- always returned: the R15 Head and UpperTorso PARTS.
--
-- A bone is not a part. Head sits at the atlas joint and UpperTorso's origin is
-- the centre of the chest box, so handing back the equivalent human bone moves
-- the eye down a third of a stud and the whole weapon with it. Everything
-- downstream -- eye relief, free-aim pivot, ADS solve -- is tuned against the
-- part frames, so the part frames are what it gets.
--
-- This costs nothing in fidelity: syncR15 has already laid those parts over the
-- human skeleton this frame, so they follow the real body.
function SoldierVisual.getHeadCF(rig: any): CFrame?
	if not rig or not rig.character then return nil end
	local part = findPart(rig.character, "Head")
	if part then return part.CFrame end
	local b = rig.solved and rig.bones and rig.bones.Head
	return b and b.world or nil
end

function SoldierVisual.getChestCF(rig: any): CFrame?
	if not rig or not rig.character then return nil end
	local part = findPart(rig.character, "UpperTorso", "Torso")
	if part then return part.CFrame end
	local b = rig.solved and rig.bones and rig.bones.Spine1
	return b and b.world or nil
end

-- Where the aiming eye actually is: in front of and above the head joint, on
-- the dominant-eye side. ADS aligns the weapon's sight line to this point.
function SoldierVisual.getEyeCF(rig: any): CFrame?
	local head = SoldierVisual.getHeadCF(rig)
	if not head then return nil end
	return head * Cfg.HUMAN_ANIM.EYE_OFFSET
end

return SoldierVisual
