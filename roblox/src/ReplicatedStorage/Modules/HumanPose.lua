--!nonstrict
-- HumanPose -- locomotion and stance on the real human skeleton.
--
-- Every rotation here is written in CHARACTER space (+X right, +Y up, +Z back)
-- and converted per bone by HumanRig.anatomical, so "pitch the thigh forward"
-- is the same expression for every joint.
--
-- What this does that the R15 version could not:
--
--   CLAVICLES   the shoulders roll forward and shrug up as the rifle comes to
--               the eye. R15 has no clavicle, so an R15 soldier aims with the
--               shoulders of someone standing at attention. This is the single
--               biggest reason the old body read as a doll holding a prop.
--   SPINE       two segments, so the soldier rounds over the gun instead of
--               hinging at one waist joint. Lean and recoil distribute across
--               both, which is also what stops the head from swinging.
--   TOES        the trailing foot rolls over the ball at push-off and the lead
--               foot lands heel-first, which is most of what makes a walk cycle
--               look weighted rather than skated.
--
-- The gait is a phase-driven cycle rather than baked keyframes: it has to stay
-- continuous while the player changes speed, direction and stance mid-step, and
-- it has to stay in sync with the weapon animation, which is also phase-driven.

local HumanRig = require(script.Parent:WaitForChild("HumanRig"))
local Cfg = require(script.Parent:WaitForChild("GameConfig"))

local HumanPose = {}

local rad = math.rad

local function approach(current: number, target: number, speed: number, dt: number): number
	return current + (target - current) * (1 - math.exp(-speed * math.min(dt, 1 / 20)))
end

function HumanPose.newState(): any
	return {
		phase = 0, speed = 0, sprint = 0, crouch = 0, ads = 0,
		airborne = 0, land = 0, wasAirborne = false,
		lean = 0, torsoYaw = 0, torsoPitch = 0,
		headYaw = 0, headPitch = 0, headRoll = 0,
		moveX = 0, moveZ = 0, dt = 1 / 60,
	}
end

--------------------------------------------------------------------------------

-- Human proportions decide how far a stride actually travels, so the gait is
-- built from the rig's measured leg length rather than tuned in the abstract.
local L = nil
local function lengths()
	if not L then L = HumanRig.lengths() end
	return L
end

function HumanPose.update(rig: any, m: any, input: any, dt: number)
	local A = Cfg.SOLDIER_ANIM
	local H = Cfg.HUMAN_ANIM
	input = input or {}
	dt = math.clamp(dt or m.dt or 1 / 60, 1 / 240, 1 / 20)
	m.dt = dt

	local lean = math.clamp(tonumber(input.lean) or 0, -1, 1)
	local moveX = math.clamp(tonumber(input.moveX) or 0, -1, 1)
	local moveZ = math.clamp(tonumber(input.moveZ) or 0, -1, 1)
	local verticalVelocity = tonumber(input.verticalVelocity) or 0

	m.speed = approach(m.speed, math.clamp(tonumber(input.speed) or 0, 0, 1.35), A.SMOOTH, dt)
	m.sprint = approach(m.sprint, input.sprint == true and 1 or 0, A.SMOOTH, dt)
	m.crouch = approach(m.crouch, math.clamp(tonumber(input.stance) or 0, 0, 1), A.SMOOTH, dt)
	m.ads = approach(m.ads, math.clamp(tonumber(input.ads) or 0, 0, 1), A.SMOOTH, dt)
	m.airborne = approach(m.airborne, input.airborne == true and 1 or 0, A.SMOOTH * 1.4, dt)
	m.lean = approach(m.lean, lean, A.SMOOTH, dt)
	m.moveX = approach(m.moveX, moveX, A.SMOOTH, dt)
	m.moveZ = approach(m.moveZ, moveZ, A.SMOOTH, dt)
	m.torsoYaw = tonumber(input.torsoYaw) or 0
	m.torsoPitch = tonumber(input.torsoPitch) or 0
	m.headYaw = tonumber(input.headYaw) or 0
	m.headPitch = tonumber(input.headPitch) or 0
	m.headRoll = tonumber(input.headRoll) or 0

	if m.wasAirborne and input.airborne ~= true then m.land = 1 end
	m.wasAirborne = input.airborne == true
	m.land = math.max(0, m.land - dt / A.LAND_TIME)

	-- ---- gait phase ------------------------------------------------------
	local cadence = A.WALK_CADENCE
	if m.sprint > 0.5 then cadence = A.SPRINT_CADENCE end
	if m.crouch > 0.5 then cadence = A.CROUCH_CADENCE end
	local energy = m.speed * (1 - m.airborne)
	m.phase += dt * cadence * math.clamp(0.25 + energy * 0.88, 0.25, 1.5)
	local phase = m.phase

	-- Legs are half a cycle apart. `swing` is +1 while the leg is travelling
	-- forward through the air and -1 while it is planted and driving back.
	local swingL = math.sin(phase)
	local swingR = -swingL
	local liftL = math.max(0, -swingL)
	local liftR = math.max(0, -swingR)

	local strideBase = A.WALK_HIP
	strideBase += (A.SPRINT_HIP - A.WALK_HIP) * m.sprint
	strideBase += (A.CROUCH_HIP - strideBase) * m.crouch
	local gait = energy * (1 - m.ads * (1 - A.ADS_GAIT_SCALE))
	local forwardWeight = math.clamp(math.abs(m.moveZ) + 0.18, 0.18, 1)
	local stride = strideBase * gait * forwardWeight
	local strafeRoll = A.STRAFE_HIP_ROLL * m.moveX * gait

	-- Walking backwards reverses which way the leg reaches.
	local dir = m.moveZ < -0.05 and -1 or 1
	local hipL = swingL * stride * dir
	local hipR = swingR * stride * dir

	local kneeMax = A.KNEE_FLEX + (A.SPRINT_KNEE - A.KNEE_FLEX) * m.sprint
	kneeMax += (A.CROUCH_KNEE - kneeMax) * m.crouch
	local kneeL = liftL * kneeMax * gait
	local kneeR = liftR * kneeMax * gait

	-- Standing crouch: both knees carry a constant bend on top of the gait.
	local crouchKnee = H.CROUCH_KNEE_HOLD * m.crouch
	kneeL += crouchKnee
	kneeR += crouchKnee
	hipL -= crouchKnee * H.CROUCH_HIP_RATIO
	hipR -= crouchKnee * H.CROUCH_HIP_RATIO

	local ankleL = -hipL * 0.34 - kneeL * 0.30
	local ankleR = -hipR * 0.34 - kneeR * 0.30

	-- Toe roll. The planted foot pushes over the ball as it leaves the ground,
	-- the swinging foot dorsiflexes so it clears and lands heel-first.
	local pushL = math.clamp(math.max(0, swingL) * 1.4, 0, 1) * gait
	local pushR = math.clamp(math.max(0, swingR) * 1.4, 0, 1) * gait
	local toeL = H.TOE_PUSH * pushL - H.TOE_LIFT * liftL * gait
	local toeR = H.TOE_PUSH * pushR - H.TOE_LIFT * liftR * gait

	if m.airborne > 0.05 then
		local ascent = math.clamp(verticalVelocity / 18, -1, 1)
		local tuck = math.max(0, ascent) * m.airborne
		local fall = math.max(0, -ascent) * m.airborne
		local blend = 1 - m.airborne
		hipL = hipL * blend + A.JUMP_TUCK_HIP * tuck + A.FALL_EXTEND_HIP * fall
		hipR = hipR * blend + A.JUMP_TUCK_HIP * tuck + A.FALL_EXTEND_HIP * fall
		kneeL = kneeL * blend + A.JUMP_TUCK_KNEE * tuck + A.FALL_KNEE * fall
		kneeR = kneeR * blend + A.JUMP_TUCK_KNEE * tuck + A.FALL_KNEE * fall
		ankleL, ankleR = -kneeL * 0.26, -kneeR * 0.26
		toeL, toeR = toeL * blend, toeR * blend
	end

	local landing = m.land * A.LAND_COMPRESS
	kneeL += landing
	kneeR += landing
	hipL += landing * 0.34
	hipR += landing * 0.34

	-- ---- pelvis ----------------------------------------------------------
	local bob = math.abs(math.cos(phase)) * A.PELVIS_BOB * gait
	local sway = math.sin(phase) * A.PELVIS_SWAY * gait
	local pelvisYaw = math.sin(phase) * A.PELVIS_YAW * gait
	-- Weight transfer: the pelvis drops toward whichever leg is unloaded.
	local pelvisRoll = math.sin(phase) * H.PELVIS_ROLL * gait

	local crouchDrop = A.CROUCH_DROP * m.crouch
	local hipsY = -bob - crouchDrop - m.land * 0.10
	local hipsX = sway + m.lean * 0.035
	local hipsZ = A.CROUCH_FORWARD * m.crouch

	HumanRig.set(rig, "Hips", HumanRig.anatomical("Hips",
		CFrame.Angles(0, pelvisYaw, rad(-3.5) * m.lean + pelvisRoll),
		Vector3.new(hipsX, hipsY, hipsZ)))

	-- ---- spine -----------------------------------------------------------
	-- Forward lean under load, split across both segments so the chest rounds
	-- over the weapon rather than the whole trunk hinging at the waist.
	local forwardLean = A.WALK_LEAN * gait + A.SPRINT_LEAN * m.sprint
	forwardLean += H.ADS_SPINE_FORWARD * m.ads
	forwardLean += H.CROUCH_SPINE_FORWARD * m.crouch
	local counter = -math.sin(phase) * A.TORSO_COUNTER * gait

	local spineShare = H.SPINE_LOWER_SHARE
	HumanRig.set(rig, "Spine", HumanRig.anatomical("Spine", CFrame.Angles(
		(m.torsoPitch * 0.30 + forwardLean) * spineShare,
		(m.torsoYaw * 0.28 + counter) * spineShare,
		rad(-9.5) * m.lean * spineShare)))

	HumanRig.set(rig, "Spine1", HumanRig.anatomical("Spine1", CFrame.Angles(
		(m.torsoPitch * 0.70 + forwardLean * 0.55) * (1 - spineShare) + H.ADS_CHEST_ROUND * m.ads,
		(m.torsoYaw * 0.72 + counter * 0.6) * (1 - spineShare),
		rad(-9.5) * m.lean * (1 - spineShare) + sway * 0.5)))

	-- ---- neck and head ---------------------------------------------------
	-- The head cancels most of what the spine just did: a real shooter keeps
	-- their eyes level and on the target while the trunk works underneath.
	local spinePitch = m.torsoPitch + forwardLean * 0.75
	HumanRig.set(rig, "Neck", HumanRig.anatomical("Neck", CFrame.Angles(
		(m.headPitch - spinePitch) * 0.45,
		(m.headYaw - m.torsoYaw) * 0.42,
		m.headRoll * 0.4 + rad(1.4) * m.lean)))
	HumanRig.set(rig, "Head", HumanRig.anatomical("Head", CFrame.Angles(
		(m.headPitch - spinePitch) * 0.55 - H.ADS_HEAD_TILT * m.ads,
		(m.headYaw - m.torsoYaw) * 0.58,
		m.headRoll * 0.6 - sway * 0.18)))

	-- ---- legs ------------------------------------------------------------
	HumanRig.set(rig, "LeftUpLeg", HumanRig.anatomical("LeftUpLeg",
		CFrame.Angles(hipL, 0, -strafeRoll - H.STANCE_WIDTH * m.crouch)))
	HumanRig.set(rig, "RightUpLeg", HumanRig.anatomical("RightUpLeg",
		CFrame.Angles(hipR, 0, -strafeRoll + H.STANCE_WIDTH * m.crouch)))
	HumanRig.set(rig, "LeftLeg", HumanRig.anatomical("LeftLeg", CFrame.Angles(-kneeL, 0, 0)))
	HumanRig.set(rig, "RightLeg", HumanRig.anatomical("RightLeg", CFrame.Angles(-kneeR, 0, 0)))
	HumanRig.set(rig, "LeftFoot", HumanRig.anatomical("LeftFoot",
		CFrame.Angles(ankleL, 0, strafeRoll * 0.45)))
	HumanRig.set(rig, "RightFoot", HumanRig.anatomical("RightFoot",
		CFrame.Angles(ankleR, 0, strafeRoll * 0.45)))
	HumanRig.set(rig, "LeftToeBase", HumanRig.anatomical("LeftToeBase", CFrame.Angles(toeL, 0, 0)))
	HumanRig.set(rig, "RightToeBase", HumanRig.anatomical("RightToeBase", CFrame.Angles(toeR, 0, 0)))

	-- ---- shoulders -------------------------------------------------------
	-- Written here so the body has a complete pose even with no weapon; the arm
	-- IK overwrites the arm chain below the clavicle when one is held.
	HumanPose.shoulders(rig, m)

	-- A relaxed arm pose for spawn, death and empty hands. Overwritten by
	-- HumanArms.solve on any frame where a weapon is present.
	local droop = H.IDLE_ARM_DROOP
	local swingArm = math.sin(phase) * H.ARM_SWING * gait
	HumanRig.set(rig, "LeftArm", HumanRig.anatomical("LeftArm",
		CFrame.Angles(-swingArm, 0, droop)))
	HumanRig.set(rig, "RightArm", HumanRig.anatomical("RightArm",
		CFrame.Angles(swingArm, 0, -droop)))
	HumanRig.set(rig, "LeftForeArm", HumanRig.anatomical("LeftForeArm",
		CFrame.Angles(-H.IDLE_ELBOW, 0, 0)))
	HumanRig.set(rig, "RightForeArm", HumanRig.anatomical("RightForeArm",
		CFrame.Angles(-H.IDLE_ELBOW, 0, 0)))
	HumanRig.set(rig, "LeftHand", CFrame.identity)
	HumanRig.set(rig, "RightHand", CFrame.identity)
end

-- Clavicles. Both roll forward and shrug up with ADS; the firing-side one also
-- pulls back to seat the stock, which is the shape that reads as "braced".
function HumanPose.shoulders(rig: any, m: any)
	local H = Cfg.HUMAN_ANIM
	local up = H.CLAVICLE_SHRUG * m.ads
	local fwd = H.CLAVICLE_FORWARD * m.ads
	local brace = H.CLAVICLE_BRACE * m.ads

	HumanRig.set(rig, "LeftShoulder", HumanRig.anatomical("LeftShoulder",
		CFrame.Angles(fwd + H.CLAVICLE_SUPPORT_LIFT * m.ads, 0, -up)))
	HumanRig.set(rig, "RightShoulder", HumanRig.anatomical("RightShoulder",
		CFrame.Angles(fwd - brace, 0, up)))
end

return HumanPose
