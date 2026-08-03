--!nonstrict
-- SoldierVisual -- one body for first person, third person and weapon posing.
--
-- V37 draws the real human body from the SWAT operator model. The R15 chain
-- below it is untouched from V31: it is posed first, evaluated forward into
-- rig.poseWorld, and read by FPSClient through getChestCF/getHeadCF to place the
-- camera and the weapon.
--
-- THAT ORDER IS LOAD-BEARING. V36 inverted it -- the human skeleton became the
-- source of the pose and R15 was slaved to follow -- and it moved the gun, for
-- two reasons worth keeping written down:
--
--   * getChestCF returned UpperTorso.CFrame. Roblox does not guarantee that
--     Part.CFrame reflects a Motor6D.Transform written earlier in the same
--     render step, which is exactly what rig.poseWorld exists to work around.
--     The weapon is placed from chestCF, so a stale chest is a moved gun.
--   * the slaving pass laid UpperTorso along Spine1 -> Neck measured downward,
--     so the chest frame came back inverted even once it did settle.
--
-- So the human body is a CONSUMER of the pose, not the producer of it. See
-- OperatorShell, which does the retarget. Everything the camera and the weapon
-- touch behaves exactly as it did before the body changed.

local ReplicatedStorage = game:GetService("ReplicatedStorage")
local CustomSoldierBuilder = require(script.Parent:WaitForChild("CustomSoldierBuilder"))
local Cfg = require(script.Parent:WaitForChild("GameConfig"))

-- The operator body is an upgrade, never a dependency. If its module or its
-- data is missing the old procedural shell still builds, which is the whole
-- reason the require is guarded: a body that fails to load must not be able to
-- take the camera and the weapon down with it.
local OperatorShell = nil
do
	local mod = script.Parent:WaitForChild("OperatorShell", 10)
	if mod then
		local ok, result = pcall(require, mod)
		if ok then
			OperatorShell = result
		else
			warn("[BLACKSITE] OperatorShell failed to load: " .. tostring(result))
		end
	end
end

local SoldierVisual = {}

-- Faults inside the operator layer are reported once each. A body that stops
-- updating is obvious; a body that was never built is not, and neither is
-- diagnosable from a client that says nothing.
local reported = {}
local function report(key: string, err: any)
	if reported[key] then return end
	reported[key] = true
	warn(("[BLACKSITE] operator body: %s failed -- %s"):format(key, tostring(err)))
end

local function isArmName(name: string): boolean
	return name == "LeftUpperArm" or name == "LeftLowerArm" or name == "LeftHand"
		or name == "RightUpperArm" or name == "RightLowerArm" or name == "RightHand"
		or name == "Left Arm" or name == "Right Arm"
end

local function isLegName(name: string): boolean
	return name == "LeftUpperLeg" or name == "LeftLowerLeg" or name == "LeftFoot"
		or name == "RightUpperLeg" or name == "RightLowerLeg" or name == "RightFoot"
		or name == "Left Leg" or name == "Right Leg"
end

local function isTorsoName(name: string): boolean
	return name == "UpperTorso" or name == "LowerTorso" or name == "Torso"
end

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

local function classify(rig: any, inst: Instance): string?
	if inst:IsA("BasePart") then
		if inst.Name == "HumanoidRootPart" then return "root" end
		if inst.Name == "Head" then return "head" end
		if isArmName(inst.Name) then return "arms" end
		if isLegName(inst.Name) then return "legs" end
		if isTorsoName(inst.Name) then return "torso" end
		local inGear = rig.gear and inst:IsDescendantOf(rig.gear)
		if not inGear then
			local a = inst.Parent
			while a and a ~= rig.character do
				if a.Name == "TacticalCharacterGear" then inGear = true break end
				a = a.Parent
			end
		end
		if inGear then
			local n = inst.Name:lower()
			if n:find("helmet") or n:find("earpro") or n:find("balaclava") or n:find("eyepro") then return "head" end
			if n:find("knee") or n:find("boot") or n:find("holster") then return "legs" end
			if n:find("glove") or n:find("sleeve") then return "arms" end
			return "gear"
		end
		if inst.Parent and inst.Parent:IsA("Accessory") then return "head" end
		return "body"
	elseif inst:IsA("Decal") or inst:IsA("Texture") then
		return "decal"
	end
	return nil
end

local function rememberInstance(rig: any, inst: Instance)
	if rig.visualModel and inst:IsDescendantOf(rig.visualModel) then return end
	local group = classify(rig, inst)
	if not group then return end
	if inst:IsA("BasePart") then
		if rig.parts[inst] == nil then
			rig.parts[inst] = { original = inst.LocalTransparencyModifier, group = group }
		end
	elseif inst:IsA("Decal") or inst:IsA("Texture") then
		if rig.decals[inst] == nil then rig.decals[inst] = inst.Transparency end
	end
end

local function disableDefaultAnimator(character: Model)
	-- BLACKSITE owns every visible joint. Roblox's default Animate script was
	-- still applying walk/run tracks underneath the custom mesh, which made the
	-- legs use Roblox timing while the weapon/body used BLACKSITE timing.
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

local function commonRig(character: Model, visualModel: Model, mode: string): any
	disableDefaultAnimator(character)
	return {
		mode = mode,
		character = character,
		visualModel = visualModel,
		gear = character:FindFirstChild("TacticalCharacterGear"),
		pieces = {},
		parts = {},
		decals = {},
		connections = {},
		-- World-space pose cache evaluated directly from the Motor6D chain.
		-- Roblox does not guarantee that Part.CFrame reflects a Transform written
		-- earlier in the same render step, which is why v34's custom body often
		-- stayed in its bind pose even though the joints were changing.
		poseWorld = {},
		applied = {
			root = CFrame.identity, waist = CFrame.identity, neck = CFrame.identity,
			leftHip = CFrame.identity, leftKnee = CFrame.identity, leftAnkle = CFrame.identity,
			rightHip = CFrame.identity, rightKnee = CFrame.identity, rightAnkle = CFrame.identity,
		},
		motion = {
			phase = 0, speed = 0, sprint = 0, crouch = 0, ads = 0,
			airborne = 0, land = 0, wasAirborne = false, dt = 1 / 60,
		},
		joints = {
			root = motor(character, "Root") or motor(character, "RootJoint"),
			waist = motor(character, "Waist"),
			neck = motor(character, "Neck"),
			leftShoulder = motor(character, "LeftShoulder") or motor(character, "Left Shoulder"),
			leftElbow = motor(character, "LeftElbow"),
			leftWrist = motor(character, "LeftWrist"),
			rightShoulder = motor(character, "RightShoulder") or motor(character, "Right Shoulder"),
			rightElbow = motor(character, "RightElbow"),
			rightWrist = motor(character, "RightWrist"),
			leftHip = motor(character, "LeftHip") or motor(character, "Left Hip"),
			leftKnee = motor(character, "LeftKnee"),
			leftAnkle = motor(character, "LeftAnkle"),
			rightHip = motor(character, "RightHip") or motor(character, "Right Hip"),
			rightKnee = motor(character, "RightKnee"),
			rightAnkle = motor(character, "RightAnkle"),
		},
	}
end

local function inferSourcePart(part: BasePart): string?
	local explicit = part:GetAttribute("SourcePart")
	if typeof(explicit) == "string" and explicit ~= "" then return explicit end
	local n = part.Name
	local prefixes = {"Gear_", "Mesh_", "Body_"}
	for _, prefix in ipairs(prefixes) do
		if n:sub(1, #prefix) == prefix then n = n:sub(#prefix + 1) end
	end
	for _, source in ipairs({
		"LeftUpperArm","LeftLowerArm","LeftHand","RightUpperArm","RightLowerArm","RightHand",
		"LeftUpperLeg","LeftLowerLeg","LeftFoot","RightUpperLeg","RightLowerLeg","RightFoot",
		"UpperTorso","LowerTorso","Head",
	}) do
		if n == source or n:find(source, 1, true) then return source end
	end
	return nil
end

local function inferVisualGroup(sourceName: string, partName: string): string
	local explicit = partName:lower()
	if sourceName == "Head" then return "head" end
	if sourceName:find("Arm") or sourceName:find("Hand") then return "arms" end
	if sourceName:find("Leg") or sourceName:find("Foot") then return "legs" end
	if explicit:find("helmet") or explicit:find("goggle") or explicit:find("balaclava") then return "head" end
	return "torso"
end

local function cloneExternalRig(character: Model, source: Model): any
	local model = source:Clone()
	model.Name = "BlacksiteSoldierVisual"
	model.Parent = character
	local rig = commonRig(character, model, "external")
	for _, d in model:GetDescendants() do
		if d:IsA("BasePart") then
			d.Anchored = true
			d.CanCollide = false
			d.CanTouch = false
			d.CanQuery = false
			d.CastShadow = true
			local sourceName = inferSourcePart(d)
			if sourceName then
				local offset = d:GetAttribute("LocalOffset")
				if typeof(offset) ~= "CFrame" then offset = CFrame.identity end
				table.insert(rig.pieces, {
					sourceName = sourceName,
					part = d,
					offset = offset,
					group = tostring(d:GetAttribute("VisualGroup") or inferVisualGroup(sourceName, d.Name)),
				})
			end
		end
	end
	for _, d in character:GetDescendants() do rememberInstance(rig, d) end
	return rig
end

-- The operator body, when it builds. Falls through to the procedural shell
-- otherwise, so the player always has a body.
local function buildOperatorRig(character: Model): any?
	if not OperatorShell then return nil end
	local visualModel = Instance.new("Model")
	visualModel.Name = "BlacksiteSoldierVisual"
	visualModel.Parent = character
	local ok, shell = pcall(OperatorShell.build, character, visualModel)
	if not ok then
		report("build", shell)
		visualModel:Destroy()
		return nil
	end
	if not shell then
		visualModel:Destroy()
		return nil
	end
	local rig = commonRig(character, visualModel, "operator")
	rig.shell = shell
	rig.customSource = shell.mode
	rig.born = os.clock()
	for _, d in character:GetDescendants() do rememberInstance(rig, d) end
	return rig
end

function SoldierVisual.build(character: Model): any
	-- Prefer an imported production rig when the owner has installed the supplied
	-- GLB. Otherwise reconstruct BLACKSITE's own custom geometry from embedded
	-- vertex data. The visible body never falls back to a Roblox avatar bundle.
	local models = ReplicatedStorage:FindFirstChild("CharacterModels")
	local external = models and models:FindFirstChild("MilitaryCustomRig")
	if external and external:IsA("Model") then
		local rig = cloneExternalRig(character, external)
		rig.customSource = "ImportedCustomRig"
		character:SetAttribute("BlacksiteVisualBody", "ImportedCustomRig")
		return rig
	end

	local operator = buildOperatorRig(character)
	if operator then
		character:SetAttribute("BlacksiteVisualBody", operator.customSource)
		-- One line, on purpose. "The body looks wrong" is not diagnosable from
		-- outside the client; which draw path was taken, and whether it actually
		-- produced geometry, answers it immediately.
		print(("[BLACKSITE] operator body: mode=%s pieces=%d")
			:format(tostring(operator.shell.mode), tonumber(operator.shell.pieces) or 0))
		return operator
	end

	local template, source = CustomSoldierBuilder.getTemplate()
	local rig = cloneExternalRig(character, template)
	rig.customSource = source
	character:SetAttribute("BlacksiteVisualBody", source)
	return rig
end

local function applyOverlay(rig: any, key: string, target: CFrame)
	local joint = rig.joints and rig.joints[key]
	if not joint or not joint.Parent then return end
	local old = rig.applied[key] or CFrame.identity
	local base = joint.Transform * old:Inverse()
	joint.Transform = base * target
	rig.applied[key] = target
end

local function evaluateJoint(rig: any, key: string, part0Name: string, part1Name: string)
	local joint = rig.joints and rig.joints[key]
	if not joint or not joint.Parent then return end
	local p0 = rig.poseWorld[part0Name]
	if not p0 then
		local part0 = findPart(rig.character, part0Name)
		p0 = part0 and part0.CFrame or nil
	end
	if not p0 then return end
	rig.poseWorld[part1Name] = p0 * joint.C0 * joint.Transform * joint.C1:Inverse()
end

-- Evaluate the full R15 hierarchy ourselves immediately after writing the
-- Motor6D transforms. This makes the custom body deterministic in the same
-- render frame instead of relying on Roblox's later animation/physics pass.
local function evaluateSkeleton(rig: any)
	if not rig or not rig.character then return end
	local rootPart = findPart(rig.character, "HumanoidRootPart")
	if not rootPart then return end
	local pw = rig.poseWorld
	pw.HumanoidRootPart = rootPart.CFrame

	evaluateJoint(rig, "root", "HumanoidRootPart", "LowerTorso")
	evaluateJoint(rig, "waist", "LowerTorso", "UpperTorso")
	evaluateJoint(rig, "neck", "UpperTorso", "Head")

	evaluateJoint(rig, "leftHip", "LowerTorso", "LeftUpperLeg")
	evaluateJoint(rig, "leftKnee", "LeftUpperLeg", "LeftLowerLeg")
	evaluateJoint(rig, "leftAnkle", "LeftLowerLeg", "LeftFoot")
	evaluateJoint(rig, "rightHip", "LowerTorso", "RightUpperLeg")
	evaluateJoint(rig, "rightKnee", "RightUpperLeg", "RightLowerLeg")
	evaluateJoint(rig, "rightAnkle", "RightLowerLeg", "RightFoot")

	-- These are overwritten by poseArms when a weapon is present, but evaluating
	-- them here gives a valid relaxed pose during spawn, death, or weapon loss.
	evaluateJoint(rig, "leftShoulder", "UpperTorso", "LeftUpperArm")
	evaluateJoint(rig, "leftElbow", "LeftUpperArm", "LeftLowerArm")
	evaluateJoint(rig, "leftWrist", "LeftLowerArm", "LeftHand")
	evaluateJoint(rig, "rightShoulder", "UpperTorso", "RightUpperArm")
	evaluateJoint(rig, "rightElbow", "RightUpperArm", "RightLowerArm")
	evaluateJoint(rig, "rightWrist", "RightLowerArm", "RightHand")
end

-- Drive the real skeleton. Lean, crouch, ADS and locomotion all share one
-- continuous body solve. The arms are solved afterward against the real weapon
-- hand targets, so the soldier never switches to an unrelated Roblox walk pose.
local function expApproach(current: number, target: number, speed: number, dt: number): number
	return current + (target - current) * (1 - math.exp(-speed * math.min(dt, 1 / 20)))
end

function SoldierVisual.poseBody(rig: any, state: any)
	if not rig or not rig.character or not rig.character.Parent then return end
	state = state or {}
	local A = Cfg.SOLDIER_ANIM
	local motion = rig.motion
	local dt = math.clamp(tonumber(state.dt) or motion.dt or 1 / 60, 1 / 240, 1 / 20)
	motion.dt = dt

	local lean = math.clamp(tonumber(state.lean) or 0, -1, 1)
	local torsoYaw = tonumber(state.torsoYaw) or 0
	local torsoPitch = tonumber(state.torsoPitch) or 0
	local headYaw = tonumber(state.headYaw) or 0
	local headPitch = tonumber(state.headPitch) or 0
	local headRoll = tonumber(state.headRoll) or 0
	local crouchTarget = math.clamp(tonumber(state.stance) or 0, 0, 1)
	local speedTarget = math.clamp(tonumber(state.speed) or 0, 0, 1.35)
	local sprintTarget = state.sprint == true and 1 or 0
	local adsTarget = math.clamp(tonumber(state.ads) or 0, 0, 1)
	local moveX = math.clamp(tonumber(state.moveX) or 0, -1, 1)
	local moveZ = math.clamp(tonumber(state.moveZ) or 0, -1, 1)
	local airborneTarget = state.airborne == true and 1 or 0
	local verticalVelocity = tonumber(state.verticalVelocity) or 0

	motion.speed = expApproach(motion.speed, speedTarget, A.SMOOTH, dt)
	motion.sprint = expApproach(motion.sprint, sprintTarget, A.SMOOTH, dt)
	motion.crouch = expApproach(motion.crouch, crouchTarget, A.SMOOTH, dt)
	motion.ads = expApproach(motion.ads, adsTarget, A.SMOOTH, dt)
	motion.airborne = expApproach(motion.airborne, airborneTarget, A.SMOOTH * 1.4, dt)

	if motion.wasAirborne and not state.airborne then motion.land = 1 end
	motion.wasAirborne = state.airborne == true
	motion.land = math.max(0, motion.land - dt / A.LAND_TIME)

	local cadence = A.WALK_CADENCE
	if motion.sprint > .5 then cadence = A.SPRINT_CADENCE end
	if motion.crouch > .5 then cadence = A.CROUCH_CADENCE end
	local gaitEnergy = motion.speed * (1 - motion.airborne)
	motion.phase += dt * cadence * math.clamp(.25 + gaitEnergy * .88, .25, 1.5)

	local phase = motion.phase
	local l = math.sin(phase)
	local r = -l
	local plantL = math.max(0, -l)
	local plantR = math.max(0, -r)
	local strideBase = A.WALK_HIP
	strideBase += (A.SPRINT_HIP - A.WALK_HIP) * motion.sprint
	strideBase += (A.CROUCH_HIP - strideBase) * motion.crouch
	local gaitScale = gaitEnergy * (1 - motion.ads * (1 - A.ADS_GAIT_SCALE))
	local forwardWeight = math.clamp(math.abs(moveZ) + .18, .18, 1)
	local stride = strideBase * gaitScale * forwardWeight
	local strafeRoll = A.STRAFE_HIP_ROLL * moveX * gaitScale

	local leftHipPitch = l * stride
	local rightHipPitch = r * stride
	local kneeMax = A.KNEE_FLEX + (A.SPRINT_KNEE - A.KNEE_FLEX) * motion.sprint
	kneeMax += (A.CROUCH_KNEE - kneeMax) * motion.crouch
	local leftKnee = plantL * kneeMax * gaitScale
	local rightKnee = plantR * kneeMax * gaitScale
	local leftAnkle = -leftHipPitch * .38 - leftKnee * .32
	local rightAnkle = -rightHipPitch * .38 - rightKnee * .32

	if motion.airborne > .05 then
		local ascent = math.clamp(verticalVelocity / 18, -1, 1)
		local tuck = math.max(0, ascent) * motion.airborne
		local fall = math.max(0, -ascent) * motion.airborne
		leftHipPitch = leftHipPitch * (1 - motion.airborne) + A.JUMP_TUCK_HIP * tuck + A.FALL_EXTEND_HIP * fall
		rightHipPitch = rightHipPitch * (1 - motion.airborne) + A.JUMP_TUCK_HIP * tuck + A.FALL_EXTEND_HIP * fall
		leftKnee = leftKnee * (1 - motion.airborne) + A.JUMP_TUCK_KNEE * tuck + A.FALL_KNEE * fall
		rightKnee = rightKnee * (1 - motion.airborne) + A.JUMP_TUCK_KNEE * tuck + A.FALL_KNEE * fall
		leftAnkle = -leftKnee * .28
		rightAnkle = -rightKnee * .28
	end

	local landCompression = motion.land * A.LAND_COMPRESS
	leftKnee += landCompression
	rightKnee += landCompression
	leftHipPitch += landCompression * .34
	rightHipPitch += landCompression * .34

	local bob = math.abs(math.cos(phase)) * A.PELVIS_BOB * gaitScale
	local sway = math.sin(phase) * A.PELVIS_SWAY * gaitScale
	local counterYaw = math.sin(phase) * A.PELVIS_YAW * gaitScale
	local forwardLean = A.WALK_LEAN * gaitScale + A.SPRINT_LEAN * motion.sprint
	local crouchDrop = A.CROUCH_DROP * motion.crouch
	local crouchForward = A.CROUCH_FORWARD * motion.crouch
	local adsForward = A.ADS_CHEST_FORWARD * motion.ads

	local rootPose = CFrame.new(
		lean * .035 + sway,
		-math.abs(lean) * .012 - bob - crouchDrop - motion.land * .10,
		-crouchForward
	) * CFrame.Angles(forwardLean * .36, counterYaw, math.rad(-3.5) * lean - sway * .55)

	local waistPose = CFrame.new(
		lean * .095,
		-math.abs(lean) * .020 - motion.crouch * .012,
		-adsForward
	) * CFrame.Angles(
		torsoPitch * .68 + forwardLean * .64 - math.sin(phase) * A.TORSO_COUNTER * gaitScale,
		torsoYaw * .82 - counterYaw * .75,
		math.rad(-9.5) * lean + sway * .72
	)

	local neckPose = CFrame.new(0, -A.ADS_HEAD_LOWER * motion.ads, 0) * CFrame.Angles(
		headPitch - torsoPitch * .68 - forwardLean * .18,
		headYaw - torsoYaw * .82,
		headRoll + math.rad(1.8) * lean - sway * .22
	)

	applyOverlay(rig, "root", rootPose)
	applyOverlay(rig, "waist", waistPose)
	applyOverlay(rig, "neck", neckPose)
	applyOverlay(rig, "leftHip", CFrame.Angles(leftHipPitch, 0, -strafeRoll))
	applyOverlay(rig, "rightHip", CFrame.Angles(rightHipPitch, 0, -strafeRoll))
	applyOverlay(rig, "leftKnee", CFrame.Angles(leftKnee, 0, 0))
	applyOverlay(rig, "rightKnee", CFrame.Angles(rightKnee, 0, 0))
	applyOverlay(rig, "leftAnkle", CFrame.Angles(leftAnkle, 0, strafeRoll * .45))
	applyOverlay(rig, "rightAnkle", CFrame.Angles(rightAnkle, 0, strafeRoll * .45))
	evaluateSkeleton(rig)

	-- Everything the camera and the weapon read is now final for this frame.
	-- Only after that does the operator body get retargeted onto it.
	if rig.shell then
		local ok, err = pcall(OperatorShell.pose, rig.shell, rig, state)
		if not ok then report("pose", err) end
	end
end

-- True distance, not distance along the camera's forward axis. See the note on
-- OperatorShell.nearPlane: the forward-axis version culls your own legs, which
-- are two studs below you and zero studs in front, and is why the first-person
-- body only ever appeared when looking straight down.
local function nearPlaneVisibility(part: BasePart, cameraCF: CFrame): number
	local toCam = cameraCF.Position - part.Position
	local dist = toCam.Magnitude
	if dist < 1e-4 then return 0 end
	local dir = toCam / dist
	local cf, half = part.CFrame, part.Size * .5
	local reach = math.abs(cf.RightVector:Dot(dir)) * half.X
		+ math.abs(cf.UpVector:Dot(dir)) * half.Y
		+ math.abs(cf.LookVector:Dot(dir)) * half.Z
	return math.clamp((dist - reach - .05) / .20, 0, 1)
end

local function visibilityFor(group: string, isLocal: boolean, torsoVis: number, legsVis: number): number
	if group == "root" then return 0 end
	if not isLocal then return 1 end
	if group == "head" or group == "decal" then return 0 end
	if group == "arms" then return 1 end
	if group == "legs" then return legsVis end
	return torsoVis
end

local function syncExternal(rig: any, cameraCF: CFrame, isLocal: boolean, torsoVis: number, legsVis: number)
	for _, entry in rig.pieces do
		local src = findPart(rig.character, entry.sourceName)
		local sourceCF = rig.poseWorld and rig.poseWorld[entry.sourceName]
		if not sourceCF and src then sourceCF = src.CFrame end
		if sourceCF and entry.part.Parent then
			entry.part.CFrame = sourceCF * entry.offset
			local vis = visibilityFor(entry.group, isLocal, torsoVis, legsVis)
			if isLocal then vis *= nearPlaneVisibility(entry.part, cameraCF) end
			entry.part.LocalTransparencyModifier = 1 - math.clamp(vis, 0, 1)
		end
	end
	-- The R15 skeleton remains for movement and hit detection but is not rendered
	-- whenever the imported custom soldier shell is available.
	for p in rig.parts do
		if p.Parent then p.LocalTransparencyModifier = 1 end
	end
end

function SoldierVisual.updateBody(rig: any, cameraCF: CFrame, isLocal: boolean)
	if not rig or not rig.visualModel or not rig.visualModel.Parent then return end
	-- Refresh the direct hierarchy in case a Roblox animation/physics pass changed
	-- any base Transform between BLACKSITE's body and weapon updates.
	evaluateSkeleton(rig)
	local down = -cameraCF.LookVector.Y
	local torsoVis = math.clamp((down + .015) / .22, 0, 1) ^ .72
	local legsVis = math.clamp((down + .19) / .22, 0, 1) ^ .62

	if rig.mode == "operator" then
		local ok, err = pcall(OperatorShell.draw, rig.shell, rig, cameraCF, isLocal, torsoVis, legsVis)
		if not ok then report("draw", err) end
		-- Report once, a second in, where the body actually IS and how much of
		-- it is on screen. "It built" and "you can see it" are different claims
		-- and three rounds were lost to only ever checking the first.
		if isLocal and not rig.told and os.clock() - (rig.born or 0) > 1 then
			rig.told = true
			local at, shown = OperatorShell.where(rig.shell)
			local head = rig.poseWorld and rig.poseWorld.Head
			print(("[BLACKSITE] operator body: at %s | %d/%d pieces visible | head %s | solve=%s")
				:format(at and tostring(at) or "nowhere", shown, rig.shell.pieces,
					head and tostring(head.Position) or "?", tostring(rig.shell.posed)))
		end
		-- Re-hide the R15 shell EVERY frame. LocalTransparencyModifier is not a
		-- setting, it is a per-frame value: Roblox's own first-person handling
		-- rewrites it on the local character, so hiding the shell once at build
		-- time lasts until the next time the camera touches it and then the old
		-- body pops back through the new one.
		for p in rig.parts do
			if p.Parent then p.LocalTransparencyModifier = 1 end
		end
		for d in rig.decals do
			if d.Parent then d.Transparency = 1 end
		end
		return
	end

	if rig.mode == "external" then
		syncExternal(rig, cameraCF, isLocal, torsoVis, legsVis)
		return
	end
	for p, info in rig.parts do
		if p.Parent then
			local vis = visibilityFor(info.group, isLocal, torsoVis, legsVis)
			if isLocal then vis *= nearPlaneVisibility(p, cameraCF) end
			p.LocalTransparencyModifier = 1 - math.clamp(vis, 0, 1)
		end
	end
	for d, original in rig.decals do
		if d.Parent then d.Transparency = isLocal and 1 or original end
	end
end

--------------------------------------------------------------------------------
-- QUERIES
--
-- FPSClient places the CAMERA and, through CHEST_TO_SHOULDER, the WEAPON from
-- these two. They must answer with this frame's pose, which is rig.poseWorld --
-- the forward evaluation done in evaluateSkeleton -- and NOT Part.CFrame, which
-- Roblox has not necessarily updated from a Transform written moments ago in
-- the same render step. Reading the parts instead is what moved the gun in V36.
--------------------------------------------------------------------------------

function SoldierVisual.getHeadCF(rig: any): CFrame?
	if not rig then return nil end
	evaluateSkeleton(rig)
	if rig.poseWorld and rig.poseWorld.Head then return rig.poseWorld.Head end
	local head = findPart(rig.character, "Head")
	return head and head.CFrame or nil
end

function SoldierVisual.getChestCF(rig: any): CFrame?
	if not rig then return nil end
	evaluateSkeleton(rig)
	if rig.poseWorld and rig.poseWorld.UpperTorso then return rig.poseWorld.UpperTorso end
	local chest = findPart(rig.character, "UpperTorso", "Torso")
	return chest and chest.CFrame or nil
end

-- Where the aiming eye actually is: in front of and above the head joint, on the
-- dominant-eye side.
function SoldierVisual.getEyeCF(rig: any): CFrame?
	local head = SoldierVisual.getHeadCF(rig)
	if not head then return nil end
	return head * Cfg.HUMAN_ANIM.EYE_OFFSET
end

local function solveElbow(shoulder: Vector3, hand: Vector3, l1: number, l2: number, pole: Vector3): Vector3
	local delta = hand - shoulder
	local distance = math.max(delta.Magnitude, .001)
	local dir = delta / distance
	local d = math.clamp(distance, math.abs(l1 - l2) + .02, l1 + l2 - .02)
	local cosA = math.clamp((l1*l1 + d*d - l2*l2) / (2*l1*d), -1, 1)
	local a = math.acos(cosA)
	local side = dir:Cross(pole)
	if side.Magnitude < .01 then side = dir:Cross(Vector3.yAxis) end
	if side.Magnitude < .01 then side = dir:Cross(Vector3.xAxis) end
	local bend = side.Unit:Cross(dir).Unit
	return shoulder + (dir * math.cos(a) + bend * math.sin(a)) * l1
end

local function segmentCF(a: Vector3, b: Vector3, rightHint: Vector3): CFrame
	local up = (b - a).Unit
	local right = rightHint - up * rightHint:Dot(up)
	if right.Magnitude < .01 then right = Vector3.xAxis - up * up.X end
	right = right.Unit
	local back = right:Cross(up).Unit
	return CFrame.fromMatrix((a + b) * .5, right, up, back)
end

local function setMotorWorld(m: Motor6D?, desiredPart1: CFrame, part0CF: CFrame?)
	if not m or not m.Parent or not m.Part0 or not m.Part1 then return end
	local base = part0CF or m.Part0.CFrame
	m.Transform = m.C0:Inverse() * base:Inverse() * desiredPart1 * m.C1
end

local function poseR15Arm(rig: any, side: string, handCF: CFrame, chestCF: CFrame, pose: any)
	local left = side == "Left"
	local shoulderMotor = rig.joints[left and "leftShoulder" or "rightShoulder"]
	local elbowMotor = rig.joints[left and "leftElbow" or "rightElbow"]
	local wristMotor = rig.joints[left and "leftWrist" or "rightWrist"]
	if not shoulderMotor or not elbowMotor or not wristMotor then return end
	if not shoulderMotor.Part0 or not shoulderMotor.Part1 or not elbowMotor.Part1 or not wristMotor.Part1 then return end

	local shoulderBase = rig.poseWorld and rig.poseWorld.UpperTorso or chestCF
	local shoulder = (shoulderBase * shoulderMotor.C0).Position
	local upperLen = math.max(.52, shoulderMotor.Part1.Size.Y * .88)
	local foreLen = math.max(.48, elbowMotor.Part1.Size.Y * .86)
	local poleLocal = left and Vector3.new(-.74,-.28,-.20) or Vector3.new(.74,-.30,-.18)
	poleLocal += left and (pose.leftPole or Vector3.zero) or (pose.rightPole or Vector3.zero)
	local pole = chestCF:VectorToWorldSpace(poleLocal).Unit
	local elbow = solveElbow(shoulder, handCF.Position, upperLen, foreLen, pole)

	local rightHint = left and -chestCF.RightVector or chestCF.RightVector
	local upperCF = segmentCF(shoulder, elbow, rightHint)
	local lowerCF = segmentCF(elbow, handCF.Position, handCF.RightVector)
	setMotorWorld(shoulderMotor, upperCF, shoulderBase)
	setMotorWorld(elbowMotor, lowerCF, upperCF)

	local foreDir = (handCF.Position - elbow).Unit
	local handRight = handCF.RightVector - foreDir * handCF.RightVector:Dot(foreDir)
	if handRight.Magnitude < .01 then handRight = rightHint end
	handRight = handRight.Unit
	local handBack = handRight:Cross(foreDir).Unit
	local finalHandCF = CFrame.fromMatrix(handCF.Position, handRight, foreDir, handBack)
	setMotorWorld(wristMotor, finalHandCF, lowerCF)

	local upperName = left and "LeftUpperArm" or "RightUpperArm"
	local lowerName = left and "LeftLowerArm" or "RightLowerArm"
	local handName = left and "LeftHand" or "RightHand"
	rig.poseWorld[upperName] = upperCF
	rig.poseWorld[lowerName] = lowerCF
	rig.poseWorld[handName] = finalHandCF
end

-- `weaponCF` and `def` are optional. With them the operator's hands are placed
-- on the weapon's own measured grip frames; without them the arms still follow
-- the R15 chain solved onto the viewmodel hand targets, exactly as before.
function SoldierVisual.poseArms(rig: any, chestCF: CFrame, handL: CFrame, handR: CFrame,
	pose: any, weaponCF: CFrame?, def: any?)
	if not rig or not rig.visualModel or not rig.visualModel.Parent then return end
	evaluateSkeleton(rig)
	-- Even an external visual body uses the invisible R15 joints as its skeleton;
	-- the custom arm meshes follow those joints in syncExternal().
	poseR15Arm(rig, "Left", handL, chestCF, pose)
	poseR15Arm(rig, "Right", handR, chestCF, pose)

	if rig.shell then
		local ok, err = pcall(OperatorShell.arms, rig.shell, rig, weaponCF, def, pose)
		if not ok then report("arms", err) end
	end
end

function SoldierVisual.destroy(rig: any)
	if not rig then return end
	for _, c in ipairs(rig.connections or {}) do c:Disconnect() end
	for key, old in rig.applied or {} do
		local joint = rig.joints and rig.joints[key]
		if joint and joint.Parent then joint.Transform = joint.Transform * old:Inverse() end
	end
	for p, info in rig.parts or {} do
		if p.Parent then p.LocalTransparencyModifier = info.original end
	end
	for d, old in rig.decals or {} do
		if d.Parent then d.Transparency = old end
	end
	if rig.shell and OperatorShell then pcall(OperatorShell.destroy, rig.shell) end
	if rig.visualModel then rig.visualModel:Destroy() end
end

return SoldierVisual
