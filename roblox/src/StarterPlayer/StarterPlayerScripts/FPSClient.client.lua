--!nonstrict
-- FPSClient -- camera, movement, and all weapon handling.
--
-- The whole point of this file is that shooting is a set of *manual actions*
-- with real durations, not a number that goes down. There is no ammo counter
-- anywhere in this game. What you have instead:
--
--   * A magazine object with a real round count, and a rig of other magazines.
--   * A chamber that either has a round in it or does not (the "+1").
--   * A bolt that locks back when you fire the last round and has to be
--     released before the gun will work again.
--   * Empty magazines are dropped. Live magazines with at least five rounds
--     are returned to the carrier before a fresh magazine is inserted.
--   * A mag check that tells you roughly what you have, because that is what
--     looking at the top of a stack of brass actually tells you.
--
-- Everything else -- lean, free look, weapon collision, breathing, the fire
-- selector -- exists to make the two seconds before contact feel like work.
--
-- NOTE ON THE VIEWMODEL: it lives under workspace.CurrentCamera, not Workspace.
-- Instances parented to the Camera render normally but are invisible to
-- raycasts and to other players, which is exactly what a viewmodel wants. It
-- also means no raycast in this file needs to filter the weapon out.

local Players = game:GetService("Players")
local UserInputService = game:GetService("UserInputService")
local RunService = game:GetService("RunService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local StarterGui = game:GetService("StarterGui")

local player = Players.LocalPlayer

local Modules = ReplicatedStorage:WaitForChild("Modules")
local Cfg = require(Modules:WaitForChild("GameConfig"))
local Weapons = require(Modules:WaitForChild("Weapons"))
local Ballistics = require(Modules:WaitForChild("Ballistics"))
local Hud = require(Modules:WaitForChild("Hud"))
local VM = require(Modules:WaitForChild("Viewmodel"))
local SoldierVisual = require(Modules:WaitForChild("SoldierVisual"))
local Animations = require(Modules:WaitForChild("Animations"))
local Fx = require(Modules:WaitForChild("ScreenFx"))
local Audio = require(Modules:WaitForChild("TacticalAudio"))

local Net = ReplicatedStorage:WaitForChild("Net")
local rFire = Net:WaitForChild("Fire") :: RemoteEvent
local rSync = Net:WaitForChild("Sync") :: UnreliableRemoteEvent
local rGear = Net:WaitForChild("Gear") :: RemoteEvent
local rInteract = Net:WaitForChild("Interact") :: RemoteEvent
local rBandage = Net:WaitForChild("Bandage") :: RemoteEvent
local rNoise = Net:WaitForChild("Noise") :: RemoteEvent
local rEffect = Net:WaitForChild("Effect") :: RemoteEvent
local rMessage = Net:WaitForChild("Message") :: RemoteEvent

local BUILD = "v32 true-body camera fix"

-- Prints once on join. If you do not see this line in Output, this script is
-- not running at all and nothing below it is either -- which would look
-- exactly like "the changes did nothing".
print(("[BLACKSITE] FPSClient %s loaded"):format(BUILD))

local rng = Random.new()
Hud.build()
task.spawn(function()
	Audio.preload(Cfg.SFX)
end)

pcall(function()
	StarterGui:SetCoreGuiEnabled(Enum.CoreGuiType.All, false)
end)

local function camera(): Camera
	return workspace.CurrentCamera
end

--------------------------------------------------------------------------------
-- Runtime weapon state
--------------------------------------------------------------------------------
type Mag = { cap: number, rounds: number }
type Runtime = {
	id: string,
	def: any,
	chambered: boolean,
	boltLocked: boolean,
	modeIndex: number,
	suppressed: boolean,
	mag: Mag,
	rig: { Mag }, -- magazine-fed weapons
	reserve: number, -- loose rounds, tube-fed weapons
}

local function newWeapon(id: string): Runtime
	local def = Weapons[id]
	local w: Runtime = {
		id = id,
		def = def,
		chambered = true,
		boltLocked = false,
		modeIndex = math.min(2, #def.fireModes), -- index 1 is SAFE; start on SEMI
		suppressed = false,
		mag = { cap = def.magCap, rounds = def.magCap },
		rig = {},
		reserve = 0,
	}
	if def.feed == "mag" then
		for _ = 1, def.magsCarried do
			table.insert(w.rig, { cap = def.magCap, rounds = def.magCap })
		end
	else
		w.reserve = def.magsCarried
	end
	return w
end

local slots: { [string]: Runtime } = {}
local currentSlot = "primary"
local function W(): Runtime
	return slots[currentSlot]
end
local function mode(): string
	local w = W()
	return w.def.fireModes[w.modeIndex]
end

--------------------------------------------------------------------------------
-- Player state
--------------------------------------------------------------------------------
local st = {
	----------------------------------------------------------------------------
	-- THE AIM CHAIN. Four bodies, each dragged by the one in front through a
	-- dead-band spring. See C.BODY for why this replaced the old single
	-- clamped deadzone. The mouse writes to aimYaw/aimPitch and nothing else.
	----------------------------------------------------------------------------
	aimYaw = 0, -- the BORE. Mouse-driven, 1:1, zero latency.
	aimPitch = 0,

	headYaw = 0, -- the CAMERA. Chases the bore.
	headPitch = 0,
	headYawVel = 0,
	headPitchVel = 0,
	headRoll = 0, -- vestibular roll into a fast turn
	headRollVel = 0,

	-- Camera POSITION follows the physical head, but camera ROTATION is built
	-- directly from look input. Reading Head.CFrame back after writing Neck/
	-- Waist Motor6D transforms created a feedback loop: pitch could disappear
	-- for a frame and the camera could jitter as the skeleton and camera fought.
	cameraEyePos = nil :: Vector3?,
	cameraEyeVel = Vector3.zero,

	torsoYaw = 0, -- the CHEST. Chases the head. Carries the weapon.
	torsoPitch = 0,
	torsoYawVel = 0,
	torsoPitchVel = 0,

	hipYaw = 0, -- the HIPS. Chase the chest. Set movement direction.
	hipYawVel = 0,

	freeYaw = 0,
	freePitch = 0,
	freeLook = false,

	sprinting = false,
	slowWalk = false,

	-- Stance is CONTINUOUS: 0 is standing, 1 is a full crouch, and every value
	-- between is reachable with the mouse. Same for lean, -1 to 1.
	stance = 0,
	stanceVel = 0,
	stanceTarget = 0,
	lean = 0,
	leanVel = 0,
	leanTarget = 0,
	analogMode = nil :: string?, -- "lean" | "crouch" while a key is held down
	analogSince = 0,
	analogMoved = false,
	analogSide = 1, -- which way Q/E is steering
	crouched = false, -- derived from stance, kept for the spread/speed lookups

	-- Arm stamina. Holding a rifle up is work; the low ready is how you rest.
	fatigue = 0,
	readyDown = false, -- G: park the weapon at low ready
	sag = 0,
	sagVel = 0,

	adsHeld = false, -- what the mouse says
	ads = false, -- what the player actually gets
	adsAlpha = 0,

	stamina = Cfg.MOVE.STAMINA_MAX,
	breath = Cfg.CAM.BREATH_MAX,
	holdBreathHeld = false,
	holdingBreath = false,

	bloom = 0,
	-- Recoil is three springs on the WEAPON now, turning about the shoulder
	-- pocket, plus a permanent component written straight into the bore. It is
	-- no longer a camera angle offset.
	recoilRise = 0,
	recoilRiseVel = 0,
	recoilSide = 0,
	recoilSideVel = 0,
	recoilBack = 0,
	recoilBackVel = 0,

	bobPhase = 0,
	idlePhase = 0,
	collide = 0, -- 0..1 how hard the muzzle is jammed into something

	torch = false,
	laser = false,

	triggerDown = false,
	firedThisPull = false,
	burstLeft = 0,
	lastShot = 0,
	weaponHeat = 0, -- drives post-burst smoke at the muzzle/suppressor

	highPort = false,
	inspecting = false,
	fov = Cfg.CAM.FOV_HIP,
	hipFov = Cfg.CAM.FOV_HIP, -- the non-aiming FOV; adsAlpha pulls off this
}

local character: Model? = nil
local humanoid: Humanoid? = nil
local hrp: BasePart? = nil
local alive = false

-- Cached so the render loop is not calling GetDescendants sixty times a second.
-- The real avatar stays authoritative for server hit detection but is hidden
-- locally; a camera-safe tactical body proxy is rendered separately.
local bodyParts: { BasePart } = {}
local fpBodyRig: any = nil

-- Declared up here rather than down in the viewmodel section because the
-- action system below closes over `anim` to cancel a timeline when a reload is
-- interrupted, and a local declared later would not be the same upvalue.
local rig: any = nil
local anim: any = nil
local meshFallbackWarned = false
local vmRootCF = CFrame.new()

-- Keep one completed rig per weapon/suppressor state. Rebuilding the runtime
-- EditableMesh every time the player changes slots can exhaust the temporary
-- mesh budget and silently fall back to the procedural box rifle. Stashing the
-- finished rig also guarantees the MK18 that comes back is byte-for-byte the
-- same model that was equipped before the pistol.
local vmCache: { [string]: any } = {}
local activeVmKey: string? = nil
local vmCacheFolder = Instance.new("Folder")
vmCacheFolder.Name = "BlacksiteClientViewmodelCache"
vmCacheFolder.Parent = ReplicatedStorage

local function splitPose(cf: CFrame): (Vector3, Vector3)
	local x, y, z = cf:ToOrientation()
	return cf.Position, Vector3.new(x, y, z)
end

local function joinPose(pos: Vector3, rot: Vector3): CFrame
	return CFrame.new(pos) * CFrame.Angles(rot.X, rot.Y, rot.Z)
end

local carryPos, carryRot = splitPose(Cfg.VIEWMODEL.HIP)
local carryPosVel, carryRotVel = Vector3.zero, Vector3.zero
local carryCF = Cfg.VIEWMODEL.HIP -- mass-rated spring result
local stanceCF = Cfg.VIEWMODEL.HIP -- carry blended toward the solved aim pose

local adsVel = 0
local prevAds = false
local shotKickPos, shotKickPosVel = Vector3.zero, Vector3.zero
local shotKickRot, shotKickRotVel = Vector3.zero, Vector3.zero

-- Camera-side motion, separate from the weapon's.
local landDip, landDipVel = 0, 0
local stepDip, stepDipVel = 0, 0
local stepRoll, stepRollVel = 0, 0
local strafeRoll, strafeRollVel = 0, 0

-- Where the muzzle points this frame. Fire direction, weapon collision and the
-- laser all use this rather than the camera: with free aim they are no longer
-- the same thing, and rounds that ignored that would make the deadzone a lie.
local aimWorldCF = CFrame.new()

-- Chest frame, exported from the render loop so the arms can hang off it.
local chestCF = CFrame.new()

-- Weapon inertia. Two spring-mass systems, one rotational and one positional,
-- both underdamped so the gun overshoots and settles. Declared up here because
-- the landing impulse is fired from a Humanoid state connection, not from the
-- render loop.
local inertiaRot, inertiaRotVel = Vector3.zero, Vector3.zero
local inertiaPos, inertiaPosVel = Vector3.zero, Vector3.zero

-- ACCELERATION, not velocity. These carry last frame's state so the render
-- loop can differentiate. Everything the weapon feels is a pseudo-force -m*a
-- in an accelerating reference frame, which is what your hands feel too --
-- and it is the difference between a walk that starts with a lurch and a walk
-- that starts with the gun already at its new offset.
local prevVelLocal = Vector3.zero
local bodyAccel = Vector3.zero -- low-passed; a raw difference of velocity spikes

--------------------------------------------------------------------------------
-- Springs
--
-- Semi-implicit Euler, but with a FIXED step. The old version used
-- h = math.min(dt, 1/30), which does not clamp the spring -- it clamps TIME.
-- Below 30 fps every spring in the game ran in slow motion, so the entire feel
-- of the weapon changed with the framerate, and the stiffest springs (the
-- pistol's ADS is k = 625) were integrating right at the edge of stability
-- even at 60. Substepping at 240 Hz costs four iterations of six additions and
-- makes the result identical at 30, 60 and 144.
--------------------------------------------------------------------------------
local function stepCount(dt: number): (number, number)
	local n = math.clamp(math.ceil(dt / Cfg.INERTIA.SUBSTEP), 1, Cfg.INERTIA.MAX_SUBSTEPS)
	return n, dt / n
end

local function springScalar(p: number, v: number, target: number, k: number, d: number, dt: number): (number, number)
	local n, h = stepCount(dt)
	for _ = 1, n do
		v += ((target - p) * k - v * d) * h
		p += v * h
	end
	return p, v
end

local function springStep(p: Vector3, v: Vector3, target: Vector3, k: number, d: number, dt: number): (Vector3, Vector3)
	local n, h = stepCount(dt)
	for _ = 1, n do
		v += ((target - p) * k - v * d) * h
		p += v * h
	end
	return p, v
end

--------------------------------------------------------------------------------
-- DEAD-BAND SPRING -- the joint that the whole body is built out of.
--
-- A joint with slack: no restoring force at all while the follower is within
-- `dz` radians of the leader, then stiffness. That is a real mechanical
-- coupling and it is what the old hard clamp should always have been.
--
-- The knee matters. A raw dead-band switches its force on discontinuously at
-- the edge, and the eye picks that up as a click. `knee` blends the force in
-- over the last stretch of the deadzone with a quadratic, so both the value
-- AND its first derivative are continuous everywhere -- the coupling fades in
-- rather than engaging.
--------------------------------------------------------------------------------
local function deadband(x: number, dz: number, knee: number): number
	local a = math.abs(x)
	local s = x < 0 and -1 or 1
	if a <= dz - knee then
		return 0
	elseif a >= dz then
		return (a - dz + knee * 0.5) * s
	end
	local t = (a - (dz - knee)) / knee
	return (t * t * knee * 0.5) * s
end

-- Follower chases `lead` through the dead band. Returns position, velocity.
-- `maxLag` is a hard backstop applied to the FOLLOWER, never to the leader --
-- clamping the leader would eat mouse input, and eating mouse input is the one
-- thing a shooter may never do.
--
-- Also returns the joint's ANALYTIC acceleration. The weapon needs angular
-- acceleration to drive its inertia, and finite-differencing a position twice
-- across a variable frame time multiplies jitter by 1/dt^2 -- at 144 fps that
-- is a factor of twenty thousand. The spring already knows its own
-- acceleration exactly, so it hands it over instead.
local function jointStep(
	p: number,
	v: number,
	lead: number,
	dz: number,
	knee: number,
	k: number,
	d: number,
	maxLag: number,
	dt: number
): (number, number, number)
	local n, h = stepCount(dt)
	local accel = 0
	for _ = 1, n do
		local ex = deadband(lead - p, dz, knee)
		accel = ex * k - v * d
		v += accel * h
		p += v * h
		local lag = lead - p
		if lag > maxLag then
			p, v = lead - maxLag, math.max(v, 0)
		elseif lag < -maxLag then
			p, v = lead + maxLag, math.min(v, 0)
		end
	end
	return p, v, accel
end

local function lerpNum(a: number, b: number, t: number): number
	return a + (b - a) * math.clamp(t, 0, 1)
end

--------------------------------------------------------------------------------
-- Timed actions. Every manual gun-handling step goes through here so anything
-- can be interrupted cleanly by sprinting, swapping or dying.
--------------------------------------------------------------------------------
local actionToken = 0
local busy = false
local busyLabel = ""

local function beginAction(label: string): number
	actionToken += 1
	busy = true
	busyLabel = label
	return actionToken
end

local function cancelAction()
	actionToken += 1
	busy = false
	busyLabel = ""
	if anim then
		anim:stop()
	end
end

-- Sleeps, then reports whether the action that started it is still the current
-- one. Every step of every reload is gated on this.
local function hold(token: number, seconds: number): boolean
	task.wait(seconds)
	return actionToken == token and alive
end

local function finish(token: number)
	if actionToken == token then
		busy = false
		busyLabel = ""
		-- Non-looping timelines end themselves; this is what stops the shotgun
		-- shell loop when the tube is finally full.
		if anim then
			anim:stop()
		end
	end
end

--------------------------------------------------------------------------------
-- Local audio. TacticalAudio owns the complete mix: coherent shot layers,
-- first reflections, room tail, occlusion, mechanism and equipment foley.
--------------------------------------------------------------------------------
local function audioExclude(): { Instance }
	local exclude: { Instance } = {}
	if character then
		table.insert(exclude, character)
	end
	local cam = workspace.CurrentCamera
	if cam then
		table.insert(exclude, cam)
	end
	return exclude
end

local function playLocal(def: any, jitter: number?)
	Audio.playLocal(def, jitter, camera(), audioExclude())
end

--------------------------------------------------------------------------------
-- Viewmodel and animation
--
-- The rig (IK arms, animatable weapon bones) lives in Viewmodel.luau and the
-- timelines live in Animations.luau. This script's only job is to say *what*
-- animation runs and *how long* it should take, so that the motion and the
-- mechanical action always agree -- the magazine leaves the well at the same
-- instant the code says the magazine has left the well.
--------------------------------------------------------------------------------
local function cacheKeyFor(slot: string, w: Runtime): string
	return slot .. (w.suppressed and ":suppressed" or ":bare")
end

local function stashActiveViewmodel()
	if rig and activeVmKey then
		rig.folder.Parent = vmCacheFolder
		vmCache[activeVmKey] = rig
	end
	rig = nil
	activeVmKey = nil
end

local function destroyAllViewmodels()
	if rig then
		VM.destroy(rig)
	end
	for _, cached in vmCache do
		if cached and cached ~= rig then
			VM.destroy(cached)
		end
	end
	table.clear(vmCache)
	rig = nil
	activeVmKey = nil
end

local function clearFirstPersonBody()
	SoldierVisual.destroy(fpBodyRig)
	fpBodyRig = nil
end

local function buildViewmodel()
	local w = W()
	if not w then
		return
	end
	local wantedKey = cacheKeyFor(currentSlot, w)
	if rig and activeVmKey == wantedKey then
		rig.folder.Parent = camera()
		return
	end
	stashActiveViewmodel()
	local cached = vmCache[wantedKey]
	if cached and cached.folder and cached.folder.Parent then
		rig = cached
		rig.folder.Parent = camera()
	else
		rig = VM.build(w.def, w.suppressed, camera(), { showInternalArms = false })
		vmCache[wantedKey] = rig
	end
	activeVmKey = wantedKey
	if anim then
		anim:setWeapon(w.def)
	else
		anim = Animations.newPlayer(w.def)
	end
end

--------------------------------------------------------------------------------
-- Shooting
--------------------------------------------------------------------------------
local function ignoreList(): { Instance }
	local t: { Instance } = {}
	if character then
		table.insert(t, character :: Instance)
	end
	return t
end

local function moveFraction(): number
	if not hrp then
		return 0
	end
	local v = (hrp :: BasePart).AssemblyLinearVelocity
	return math.clamp(Vector3.new(v.X, 0, v.Z).Magnitude / Cfg.MOVE.SPRINT, 0, 1)
end

local function footstepDef(): any
	if not hrp then
		return Cfg.SFX.STEP_CONCRETE or Cfg.SFX.STEP_HARD
	end
	local params = RaycastParams.new()
	params.FilterType = Enum.RaycastFilterType.Exclude
	params.FilterDescendantsInstances = ignoreList()
	params.IgnoreWater = false
	local result = workspace:Raycast((hrp :: BasePart).Position, Vector3.new(0, -5.5, 0), params)
	if not result then
		return Cfg.SFX.STEP_CONCRETE or Cfg.SFX.STEP_HARD
	end
	local material = result.Material
	if material == Enum.Material.Metal or material == Enum.Material.DiamondPlate or material == Enum.Material.CorrodedMetal then
		return Cfg.SFX.STEP_METAL
	elseif material == Enum.Material.Wood or material == Enum.Material.WoodPlanks then
		return Cfg.SFX.STEP_WOOD
	elseif material == Enum.Material.Grass or material == Enum.Material.Ground or material == Enum.Material.Mud or material == Enum.Material.Sand then
		return Cfg.SFX.STEP_DIRT
	elseif material == Enum.Material.Pebble or material == Enum.Material.Rock or material == Enum.Material.Slate then
		return Cfg.SFX.STEP_GRAVEL
	end
	return Cfg.SFX.STEP_CONCRETE or Cfg.SFX.STEP_HARD
end

local function limbRatio(group: string): number
	local maximum = Cfg.HEALTH.LIMB_MAX[group] or 100
	local value = player:GetAttribute("Limb_" .. group)
	if typeof(value) ~= "number" then return 1 end
	return math.clamp(value / math.max(maximum, 1), 0, 1)
end

local function traumaNumber(name: string, fallback: number?): number
	local value = player:GetAttribute(name)
	return typeof(value) == "number" and value or (fallback or 0)
end

local function fracture(group: string): number
	return math.clamp(traumaNumber("Fracture_" .. group, 0), 0, 3)
end

local FRACTURE_FUNCTION = { [0]=1.00, [1]=0.82, [2]=0.48, [3]=0.16 }

local function legMobility(): number
	local left = limbRatio("LeftLeg")
	local right = limbRatio("RightLeg")
	local worst = math.min(left, right)
	local average = (left + right) * 0.5
	local tissue = 0.35 + 0.65 * (worst * 0.70 + average * 0.30)
	local leftBone = FRACTURE_FUNCTION[fracture("LeftLeg")] or .16
	local rightBone = FRACTURE_FUNCTION[fracture("RightLeg")] or .16
	local axial = FRACTURE_FUNCTION[fracture("Axial")] or .16
	local shock = math.clamp(traumaNumber("Shock", 0), 0, 1)
	local skeletal = math.min(leftBone, rightBone, axial)
	return math.clamp(tissue * skeletal * (1 - shock * .30), .10, 1)
end

local function canSprintWithLegs(): boolean
	return math.min(limbRatio("LeftLeg"), limbRatio("RightLeg")) >= Cfg.HEALTH.SPRINT_LEG_MIN
		and fracture("LeftLeg") < 2
		and fracture("RightLeg") < 2
		and fracture("Axial") < 2
		and traumaNumber("Shock", 0) < .62
end

local function armControl(): number
	local right = limbRatio("RightArm")
	local left = limbRatio("LeftArm")
	local tissue = math.clamp(right * 0.65 + math.min(left, right) * 0.35, 0, 1)
	local rightBone = FRACTURE_FUNCTION[fracture("RightArm")] or .16
	local leftBone = FRACTURE_FUNCTION[fracture("LeftArm")] or .16
	local skeletal = rightBone * .68 + math.min(leftBone, rightBone) * .32
	local pain = math.clamp(traumaNumber("Pain", 0) / 100, 0, 1)
	return math.clamp(tissue * skeletal * (1 - pain * .20), .10, 1)
end

local function armPenalty(maximum: number): number
	return 1 + (1 - armControl()) * (maximum - 1)
end

local function worstFracture(): number
	return math.max(
		fracture("LeftLeg"), fracture("RightLeg"),
		fracture("LeftArm"), fracture("RightArm"), fracture("Axial")
	)
end

local function tracer(from: Vector3, to: Vector3)
	local delta = to - from
	local dist = delta.Magnitude
	if dist < 7 then return end
	local dir = delta / dist
	local B = Cfg.BALLISTICS
	local speed = B.TRACER_SPEED
	local streakLength = rng:NextNumber(B.TRACER_LENGTH_MIN, B.TRACER_LENGTH_MAX)
	local fade = math.clamp(dist / B.TRACER_FADE_DISTANCE, 0, 1)
	local width = B.TRACER_WIDTH_NEAR + (B.TRACER_WIDTH_FAR-B.TRACER_WIDTH_NEAR)*fade
	local p = Instance.new('Part')
	p.Name='TracerSegment'; p.Anchored=true; p.CanCollide=false; p.CanQuery=false; p.CanTouch=false; p.CastShadow=false
	p.Material=Enum.Material.Neon; p.Color=Color3.fromRGB(255,214,154); p.Size=Vector3.new(width,width,.01); p.Transparency=.48+fade*.18; p.Parent=workspace
	task.spawn(function()
		local elapsed=0
		while p.Parent do
			local dt=RunService.RenderStepped:Wait(); elapsed += math.min(dt,1/30)
			local rawHead=elapsed*speed; local headD=math.min(rawHead,dist); local tailD=math.clamp(rawHead-streakLength,0,dist)
			if tailD>=dist then break end
			local head=from+dir*headD; local tail=from+dir*tailD; local len=(head-tail).Magnitude
			if len>.01 then
				p.Size=Vector3.new(width,width,len); p.CFrame=CFrame.lookAt(tail,head)*CFrame.new(0,0,-len*.5)
				local enter=math.clamp(rawHead/math.max(streakLength*.65,.01),0,1); local leave=math.clamp((dist-tailD)/math.max(streakLength,.01),0,1)
				p.Transparency=1-(1-(.48+fade*.18))*enter*leave
			end
		end
		p:Destroy()
	end)
end

local function impactFx(hit: any)
	if not hit or not hit.instance or not hit.instance.Parent then return end
	local material = hit.instance.Material
	local metal = material==Enum.Material.Metal or material==Enum.Material.DiamondPlate or material==Enum.Material.CorrodedMetal
	local wood = material==Enum.Material.Wood or material==Enum.Material.WoodPlanks
	local dirt = material==Enum.Material.Grass or material==Enum.Material.Ground or material==Enum.Material.Mud or material==Enum.Material.Sand
	local anchor=Instance.new('Part'); anchor.Name='BulletImpact'; anchor.Size=Vector3.new(.055,.055,.012); anchor.Anchored=true; anchor.CanCollide=false; anchor.CanQuery=false; anchor.CanTouch=false; anchor.CastShadow=false
	anchor.Material=metal and Enum.Material.Neon or Enum.Material.SmoothPlastic
	anchor.Color=metal and Color3.fromRGB(255,190,92) or (wood and Color3.fromRGB(92,67,42) or (dirt and Color3.fromRGB(91,78,58) or Color3.fromRGB(55,55,55)))
	anchor.CFrame=CFrame.lookAt(hit.position+hit.normal*.008,hit.position+hit.normal)*CFrame.Angles(math.rad(90),0,0)
	anchor.Transparency=metal and .15 or .35; anchor.Parent=workspace
	local att=Instance.new('Attachment'); att.Parent=anchor
	local em=Instance.new('ParticleEmitter'); em.Texture=metal and 'rbxasset://textures/particles/sparkles_main.dds' or 'rbxasset://textures/particles/smoke_main.dds'
	em.Color=ColorSequence.new(anchor.Color); em.LightEmission=metal and .85 or 0; em.Lifetime=NumberRange.new(.10,.24); em.Speed=NumberRange.new(metal and 5 or 1.2,metal and 12 or 3.5)
	em.SpreadAngle=Vector2.new(55,55); em.Drag=metal and 2 or 5; em.Rate=0; em.Rotation=NumberRange.new(0,360); em.RotSpeed=NumberRange.new(-180,180)
	em.Size=NumberSequence.new({NumberSequenceKeypoint.new(0,metal and .035 or .07),NumberSequenceKeypoint.new(1,metal and 0 or .26)})
	em.Transparency=NumberSequence.new({NumberSequenceKeypoint.new(0,.10),NumberSequenceKeypoint.new(1,1)}); em.Parent=att; em:Emit(metal and 7 or 4)
	task.delay(metal and .35 or 8,function() if anchor.Parent then anchor:Destroy() end end)
end

local shotCount = 0
-- Re-rolled on every shot; VM.update fans the flash petals around it.
local flashRoll = 0
local flashSerial = 0 -- prevents an older shot's delayed hide from killing a newer flash
local debugFx = false -- F3

-- Ejected case. Unanchored and left to the physics engine, because a case
-- tumbling off a wall is free and hand-animating one would look worse.
local function ejectBrass()
	if not rig then
		return
	end
	local MZ = Cfg.MUZZLE
	-- The port is up and to the right of the bore, roughly under the eye.
	local portCF = vmRootCF * CFrame.new(0.13, 0.30, -0.06)

	local b = Instance.new("Part")
	b.Size = MZ.BRASS_SIZE
	b.Color = MZ.BRASS_COLOR
	b.Material = Enum.Material.Metal
	b.CanCollide = false -- keeps it off the player's own capsule
	b.CanQuery = false
	b.CastShadow = false
	b.CFrame = portCF * CFrame.Angles(rng:NextNumber(0, 6), rng:NextNumber(0, 6), 0)
	b.Parent = workspace

	-- Out to the right and slightly back, which is where an AR throws them.
	local dir = portCF.RightVector * rng:NextNumber(0.7, 1.1)
		+ portCF.UpVector * rng:NextNumber(0.25, 0.55)
		+ portCF.LookVector * rng:NextNumber(-0.35, -0.1)
	b.AssemblyLinearVelocity = dir.Unit * MZ.BRASS_SPEED * rng:NextNumber(0.8, 1.2)
	b.AssemblyAngularVelocity = Vector3.new(
		rng:NextNumber(-1, 1),
		rng:NextNumber(-1, 1),
		rng:NextNumber(-1, 1)
	).Unit * MZ.BRASS_SPIN

	task.delay(MZ.BRASS_LIFE, function()
		b:Destroy()
	end)
end

local function fireOnce()
	local w = W()
	local def = w.def

	-- Dry. The trigger moves and nothing happens. This is the single most
	-- important piece of feedback in the game, so it is deliberately not
	-- foreshadowed by any counter.
	if not w.chambered then
		playLocal(Cfg.SFX.DRY_FIRE)
		st.firedThisPull = true
		st.burstLeft = 0
		if w.boltLocked then
			Hud.say("Bolt's back. Empty.", 2, Hud.COLORS.WARN)
		end
		return
	end

	-- Rounds leave along the WEAPON's axis, not the camera's. With free aim
	-- those differ by up to nine degrees, and pretending otherwise would make
	-- the deadzone a lie.
	local aim = aimWorldCF
	w.chambered = false
	st.lastShot = os.clock()
	st.firedThisPull = true
	shotCount += 1

	local origin = aim.Position + aim.LookVector * 1.2
	local spread = Weapons.spread(def, st.ads, moveFraction(), st.bloom, st.crouched and "crouch" or "stand")

	local dirs: { Vector3 } = {}
	for _ = 1, def.pellets do
		table.insert(dirs, Ballistics.applySpread(aim.LookVector, spread, rng))
	end
	rFire:FireServer(origin, dirs, w.id, w.suppressed)

	playLocal(w.suppressed and Cfg.SFX.SHOT_SUPPRESSED or Cfg.SFX[def.shotSfx], 0.06)
	if rig then
		local MZ = Cfg.MUZZLE
		local flash, bloom, light = rig.flash, rig.flashBloom, rig.flashLight
		local supp = w.suppressed and MZ.SUPPRESSED_FLASH or 1
		local weaponScale = def.feed == "tube" and 1.18 or (def.id == "P320" and 0.82 or 1)

		flashSerial += 1
		local serial = flashSerial
		flashRoll = rng:NextNumber(0, math.pi * 2)

		-- Muzzle flame is intermittent in daylight and almost absent through a
		-- suppressor. When it does appear it is a narrow directional gas tongue,
		-- never a white sphere.
		local flashChance = w.suppressed and MZ.SUPPRESSED_FLASH_CHANCE or MZ.FLASH_CHANCE
		local showFlash = rng:NextNumber(0, 1) <= flashChance
		if showFlash and rig.flashEmitter then
			rig.flashEmitter:Emit(w.suppressed and 1 or (def.feed == "tube" and 3 or 2))
		end
		local coreScale = rng:NextNumber(0.82, 1.16) * supp * weaponScale
		flash.Size = Vector3.new(
			MZ.FLASH_CORE_THICK * coreScale,
			MZ.FLASH_CORE_THICK * coreScale,
			MZ.FLASH_CORE_LENGTH * coreScale
		)
		flash.Transparency = showFlash and (w.suppressed and 0.72 or 0.03) or 1
		bloom.Size = Vector3.new(
			MZ.FLASH_GAS_THICK * coreScale,
			MZ.FLASH_GAS_THICK * coreScale,
			MZ.FLASH_GAS_LENGTH * coreScale
		)
		bloom.Transparency = showFlash and (w.suppressed and 0.88 or 0.40) or 1
		for _, p in rig.flashPetals do
			local length = rng:NextNumber(MZ.FLASH_LOBE_MIN, MZ.FLASH_LOBE_MAX) * supp * weaponScale
			local thick = MZ.FLASH_LOBE_THICK * rng:NextNumber(0.78, 1.18) * math.max(supp, 0.52)
			p.Size = Vector3.new(thick, thick, length)
			p.Transparency = showFlash and (w.suppressed and 0.92 or rng:NextNumber(0.28, 0.50)) or 1
		end

		light.Brightness = MZ.FLASH_LIGHT * supp * weaponScale
		light.Enabled = showFlash

		local blastMult = w.suppressed and MZ.SUPPRESSED_BLAST_SMOKE or 1
		local lingerMult = w.suppressed and MZ.SUPPRESSED_LINGER_SMOKE or 1
		if rig.blastSmoke then
			local count = math.floor(rng:NextInteger(MZ.BLAST_SMOKE_MIN, MZ.BLAST_SMOKE_MAX) * blastMult * weaponScale + 0.5)
			if count > 0 then
				rig.blastSmoke:Emit(count)
			end
		end
		if rig.smoke then
			local count = math.floor(rng:NextInteger(MZ.LINGER_SMOKE_MIN, MZ.LINGER_SMOKE_MAX) * lingerMult * weaponScale + 0.5)
			if count > 0 then
				rig.smoke:Emit(count)
			end
		end

		st.weaponHeat = math.min(1.5, st.weaponHeat
			+ MZ.HEAT_PER_SHOT * weaponScale * (w.suppressed and MZ.SUPPRESSED_HEAT or 1))

		-- Hide in two stages. Token checks are essential during automatic fire: an
		-- old delayed callback must never extinguish a newer shot's flash.
		task.delay(MZ.FLASH_CORE_TIME, function()
			if serial == flashSerial and rig and rig.flash == flash then
				flash.Transparency = 1
				bloom.Transparency = 1
				light.Enabled = false
			end
		end)
		task.delay(MZ.FLASH_AFTER_TIME, function()
			if serial == flashSerial and rig and rig.flash == flash then
				for _, p in rig.flashPetals do
					p.Transparency = 1
				end
			end
		end)

		-- Brass. Only autoloaders eject on the shot; the pump throws its case
		-- when the fore-end comes back, which the pump animation covers.
		if def.feed ~= "tube" then
			ejectBrass()
		end
	end

	local visualHits = Ballistics.fire(origin, dirs[1], ignoreList(), false)
	local visualEnd = #visualHits > 0 and visualHits[1].position or (origin + dirs[1] * Cfg.BALLISTICS.MAX_RANGE)
	if #visualHits > 0 then impactFx(visualHits[1]) end
	if shotCount % Cfg.BALLISTICS.TRACER_EVERY == 0 and rig then
		local muzzleWorld = (vmRootCF * CFrame.new(rig.muzzle)).Position
		tracer(muzzleWorld, visualEnd)
	end

	-- Recoil, in the three places a rifle actually puts it. See C.RECOIL.
	local RC = Cfg.RECOIL

	local controlPenalty = armPenalty(Cfg.HEALTH.ARM_RECOIL_MAX)

	-- 1. Permanent climb, written into the BORE. Nothing gives this back.
	st.aimPitch = math.clamp(
		st.aimPitch + def.recoilPitch * RC.PERMANENT * controlPenalty,
		-Cfg.CAM.PITCH_LIMIT,
		Cfg.CAM.PITCH_LIMIT
	)
	st.aimYaw += rng:NextNumber(-def.recoilYaw, def.recoilYaw) * RC.PERMANENT * controlPenalty

	-- 2. Transient rotation of the weapon about its shoulder pocket.
	st.recoilRiseVel += def.recoilPitch * RC.RISE_IMPULSE * controlPenalty
	st.recoilSideVel += rng:NextNumber(-def.recoilYaw, def.recoilYaw) * RC.SIDE_IMPULSE * controlPenalty
	st.recoilBackVel += def.recoilKick * RC.BACK_IMPULSE * (0.85 + controlPenalty * 0.15)

	-- High-frequency impulse: the first 40-70 ms of a shot are much faster than
	-- the main recoil spring. Separating this snap from the slower shoulder
	-- recovery is what makes a shot feel like an ignition instead of a camera bob.
	shotKickPosVel += Vector3.new(
		rng:NextNumber(-RC.SNAP_POS_X, RC.SNAP_POS_X),
		-RC.SNAP_POS_Y,
		def.recoilKick * RC.SNAP_BACK
	) * controlPenalty
	shotKickRotVel += Vector3.new(
		def.recoilPitch * RC.SNAP_PITCH,
		rng:NextNumber(-def.recoilYaw, def.recoilYaw) * RC.SNAP_YAW,
		rng:NextNumber(-def.recoilYaw, def.recoilYaw) * RC.SNAP_ROLL
	) * controlPenalty

	-- 3. Straight back through the body. Your chest was hit, so your view rocks.
	st.headPitchVel += def.recoilPitch * RC.HEAD_IMPULSE
	st.torsoPitchVel += def.recoilPitch * RC.TORSO_IMPULSE

	st.bloom += def.spreadBloom

	-- Muzzle blast on the screen. Weighted by the weapon's own recoil so the
	-- shotgun hits far harder than the pistol without a per-weapon constant.
	Fx.fired(math.clamp(def.recoilPitch / Cfg.RECOIL.MAX_RISE * 2.2, 0.35, 1.6))
	-- View kick on top of recoil. Recoil rotates the WEAPON about its pocket;
	-- this is the separate jolt your head takes from the blast.
	st.headPitchVel += Cfg.SCREENFX.FIRE_SHAKE
	st.headYawVel += rng:NextNumber(-0.5, 0.5) * Cfg.SCREENFX.FIRE_SHAKE

	-- Cycle the action.
	if def.feed == "tube" then
		-- Pump guns cycle by hand. This is the whole character of the weapon:
		-- you are locked out for half a second after every shot, and you watch
		-- your own hand do it.
		local token = beginAction("pump")
		anim:play("pump", Cfg.HANDLING.PUMP_TIME)
		task.spawn(function()
			if not hold(token, Cfg.HANDLING.PUMP_BACK) then
				return
			end
			playLocal(Cfg.SFX.BOLT, 0.05)
			if not hold(token, Cfg.HANDLING.PUMP_TIME - Cfg.HANDLING.PUMP_BACK) then
				return
			end
			if w.mag.rounds > 0 then
				w.mag.rounds -= 1
				w.chambered = true
			end
			finish(token)
		end)
	else
		-- Autoloaders cycle far too fast to keyframe; the bolt kick is a short
		-- ramp the animation player adds on top of whatever else is playing.
		anim:kickBolt()
		if w.mag.rounds > 0 then
			w.mag.rounds -= 1
			w.chambered = true
		else
			w.boltLocked = true -- last round out, bolt holds open
		end
	end
end

local function canFire(): boolean
	if not alive or st.sprinting or st.highPort or st.inspecting then
		return false
	end
	-- A shattered firing arm cannot maintain a trigger grip. A shattered
	-- support arm makes a shouldered primary unusable, while the sidearm can
	-- still be fired one-handed with severe control penalties.
	if fracture("RightArm") >= 3 then return false end
	if W().def.class == "primary" and fracture("LeftArm") >= 3 then return false end
	if busy then
		-- Loading shells into a tube can be broken off to shoot, provided there
		-- is already something in the chamber. Nothing else can be rushed.
		if busyLabel ~= "shells" or not W().chambered then
			return false
		end
	end
	return mode() ~= "SAFE"
end

local function tryFire()
	if not canFire() then
		return
	end
	if os.clock() - st.lastShot < Weapons.cycleTime(W().def) then
		return
	end

	local m = mode()
	if m == "SEMI" then
		if st.firedThisPull then
			return -- one round per trigger pull, as it should be
		end
		fireOnce()
	elseif m == "BURST" then
		if st.burstLeft <= 0 then
			return
		end
		st.burstLeft -= 1
		fireOnce()
	else
		fireOnce()
	end
end

--------------------------------------------------------------------------------
-- Reloads and checks
--------------------------------------------------------------------------------
local function bestMag(rig: { Mag }): number?
	local bestIdx, best = nil, -1
	for i, m in rig do
		if m.rounds > best then
			bestIdx, best = i, m.rounds
		end
	end
	return bestIdx
end

local function dropMag()
	if not hrp then
		return
	end
	local p = Instance.new("Part")
	p.Size = Vector3.new(0.5, 1.6, 0.7)
	p.Color = Color3.fromRGB(38, 40, 37)
	p.Material = Enum.Material.Metal
	p.CFrame = (hrp :: BasePart).CFrame * CFrame.new(rng:NextNumber(-1, 1), -1.6, -1.5)
	p.Parent = workspace

	playLocal(Cfg.SFX.MAG_DROP, 0.1)
	rNoise:FireServer(p.Position, Cfg.NOISE.MAG_DROP)
	task.delay(45, function()
		p:Destroy()
	end)
end

local RELOAD_EVENT_PROFILES: { [string]: any } = {
	-- MK18 tactical techniques
	pouch_stow = { magOut = 0.20, acquire = 0.61, magIn = 0.83, tap = 0.86 },
	l_index_retention = { acquire = 0.20, magOut = 0.52, magIn = 0.69, tap = 0.81 },
	cross_body = { magOut = 0.18, acquire = 0.64, magIn = 0.82 },
	muzzle_up_workspace = { magOut = 0.20, acquire = 0.61, magIn = 0.75, tap = 0.86 },
	seat_check = { magOut = 0.18, acquire = 0.63, magIn = 0.79, tap = 0.90 },
	fast_l_index = { acquire = 0.16, magOut = 0.47, magIn = 0.66 },
	magwell_fumble = { magOut = 0.18, acquire = 0.60, contact = 0.72, magIn = 0.87 },
	pouch_snag = { magOut = 0.18, acquire = 0.66, magIn = 0.85 },

	-- Rifle empty / emergency techniques
	drop_bolt_paddle = { magOut = 0.11, acquire = 0.34, magIn = 0.71, bolt = 0.86 },
	high_drop_bolt_paddle = { magOut = 0.12, acquire = 0.27, magIn = 0.69, bolt = 0.87 },
	empty_magwell_miss = { magOut = 0.10, acquire = 0.33, contact = 0.69, magIn = 0.82, bolt = 0.91 },
	charging_handle_finish = { magOut = 0.10, acquire = 0.30, magIn = 0.66, bolt = 0.88, charge = true },
	seat_check_bolt_paddle = { magOut = 0.10, acquire = 0.31, magIn = 0.68, tap = 0.78, bolt = 0.90 },
	empty_pouch_snag = { magOut = 0.10, acquire = 0.48, magIn = 0.72, bolt = 0.88 },
	straight_drop = { magOut = 0.10, acquire = 0.27, magIn = 0.69 },
	emergency_magwell_miss = { magOut = 0.10, acquire = 0.30, contact = 0.70, magIn = 0.82 },
	fast_drop = { magOut = 0.08, acquire = 0.22, magIn = 0.63 },
	drop_seat_check = { magOut = 0.10, acquire = 0.30, magIn = 0.68, tap = 0.78 },

	-- Animation-name fallbacks for the P320.
	reload_tactical = { magOut = 0.18, acquire = 0.50, magIn = 0.82, tap = 0.87 },
	reload_tactical_b = { magOut = 0.18, acquire = 0.45, magIn = 0.80 },
	reload_tactical_c = { magOut = 0.18, acquire = 0.49, contact = 0.72, magIn = 0.88 },
	reload_tactical_d = { magOut = 0.18, acquire = 0.61, magIn = 0.84 },
	reload_tactical_e = { magOut = 0.18, acquire = 0.51, magIn = 0.80, tap = 0.91 },
	reload_tactical_f = { magOut = 0.17, acquire = 0.43, magIn = 0.76 },
	reload_empty = { magOut = 0.10, acquire = 0.30, magIn = 0.72, bolt = 0.86 },
	reload_empty_b = { magOut = 0.10, acquire = 0.27, magIn = 0.69, bolt = 0.86 },
	reload_empty_c = { magOut = 0.10, acquire = 0.31, contact = 0.66, magIn = 0.80, bolt = 0.90 },
	reload_empty_d = { magOut = 0.10, acquire = 0.28, magIn = 0.68, bolt = 0.88, charge = true },
	reload_empty_e = { magOut = 0.10, acquire = 0.30, magIn = 0.70, tap = 0.78, bolt = 0.90 },
	reload_emergency = { magOut = 0.09, acquire = 0.27, magIn = 0.70 },
	reload_emergency_b = { magOut = 0.09, acquire = 0.29, contact = 0.68, magIn = 0.82 },
	reload_emergency_c = { magOut = 0.07, acquire = 0.22, magIn = 0.63 },
	reload_emergency_d = { magOut = 0.09, acquire = 0.28, magIn = 0.70, tap = 0.79 },
}

-- V15 MK18 technique timing. These fractions match the ten clean motion
-- graphs plus two rare interruptions above; sound and mechanical state now
-- follow the selected technique instead of one generic reload schedule.
RELOAD_EVENT_PROFILES.fresh_first_l_index = { acquire = 0.16, magOut = 0.43, magIn = 0.72 }
RELOAD_EVENT_PROFILES.pouch_stow_human = { magOut = 0.20, acquire = 0.49, magIn = 0.76 }
RELOAD_EVENT_PROFILES.cross_body_human = { magOut = 0.20, acquire = 0.51, magIn = 0.77 }
RELOAD_EVENT_PROFILES.muzzle_up_human = { magOut = 0.22, acquire = 0.52, magIn = 0.78 }
RELOAD_EVENT_PROFILES.support_side_human = { magOut = 0.19, acquire = 0.47, magIn = 0.74, tap = 0.83 }
RELOAD_EVENT_PROFILES.belt_line_retention = { magOut = 0.18, acquire = 0.47, magIn = 0.75 }
RELOAD_EVENT_PROFILES.high_chest_tactile = { magOut = 0.21, acquire = 0.50, magIn = 0.73, tap = 0.82 }
RELOAD_EVENT_PROFILES.reverse_l_index = { acquire = 0.17, magOut = 0.45, magIn = 0.74 }
RELOAD_EVENT_PROFILES.under_arm_retention = { magOut = 0.19, acquire = 0.48, magIn = 0.75 }
RELOAD_EVENT_PROFILES.fast_retention_human = { magOut = 0.17, acquire = 0.45, magIn = 0.73 }
RELOAD_EVENT_PROFILES.magwell_fumble_human = { magOut = 0.19, acquire = 0.47, contact = 0.69, magIn = 0.82 }
RELOAD_EVENT_PROFILES.pouch_snag_human = { magOut = 0.19, acquire = 0.58, magIn = 0.83 }

-- V16 empty-combat reload timings. These are the twelve techniques used after
-- the bolt locks rearward; events, sounds and mechanics follow the selected
-- choreography instead of the old generic empty profile.
RELOAD_EVENT_PROFILES.combat_standard_bolt = { magOut = 0.10, acquire = 0.27, magIn = 0.70, bolt = 0.84 }
RELOAD_EVENT_PROFILES.combat_compact_bolt = { magOut = 0.09, acquire = 0.25, magIn = 0.68, bolt = 0.83 }
RELOAD_EVENT_PROFILES.combat_support_roll = { magOut = 0.10, acquire = 0.28, magIn = 0.71, bolt = 0.85 }
RELOAD_EVENT_PROFILES.combat_high_workspace = { magOut = 0.11, acquire = 0.29, magIn = 0.72, bolt = 0.86 }
RELOAD_EVENT_PROFILES.combat_centerline = { magOut = 0.09, acquire = 0.26, magIn = 0.69, bolt = 0.84 }
RELOAD_EVENT_PROFILES.combat_wide_pouch = { magOut = 0.10, acquire = 0.30, magIn = 0.72, bolt = 0.86 }
RELOAD_EVENT_PROFILES.combat_push_pull = { magOut = 0.10, acquire = 0.27, magIn = 0.68, tap = 0.755, bolt = 0.86 }
RELOAD_EVENT_PROFILES.combat_magwell_glance = { magOut = 0.10, acquire = 0.28, magIn = 0.71, bolt = 0.85 }
RELOAD_EVENT_PROFILES.combat_underarm = { magOut = 0.09, acquire = 0.27, magIn = 0.70, bolt = 0.85 }
RELOAD_EVENT_PROFILES.combat_tactile_seat = { magOut = 0.10, acquire = 0.28, magIn = 0.70, tap = 0.80, bolt = 0.87 }
RELOAD_EVENT_PROFILES.combat_charging_handle_fallback = { magOut = 0.10, acquire = 0.27, magIn = 0.69, bolt = 0.84, charge = true }
RELOAD_EVENT_PROFILES.combat_fast_bolt = { magOut = 0.08, acquire = 0.23, magIn = 0.66, bolt = 0.81 }

-- V17 supplied-video reference timings. Mechanical events follow the exact
-- selected motion rather than the generic reload schedule.
RELOAD_EVENT_PROFILES.ref_operator_compact_paddle = { magOut=.10, acquire=.28, magIn=.70, bolt=.84 }
RELOAD_EVENT_PROFILES.ref_mwii_high_charge = { magOut=.10, acquire=.30, magIn=.71, bolt=.84, charge=true }
RELOAD_EVENT_PROFILES.ref_mk18_support_roll = { magOut=.09, acquire=.27, magIn=.69, bolt=.84 }
RELOAD_EVENT_PROFILES.ref_centerline_paddle = { magOut=.09, acquire=.25, magIn=.68, bolt=.82 }
RELOAD_EVENT_PROFILES.ref_inboard_chest = { magOut=.10, acquire=.29, magIn=.71, bolt=.85 }
RELOAD_EVENT_PROFILES.ref_outboard_index = { magOut=.10, acquire=.28, magIn=.71, bolt=.85 }
RELOAD_EVENT_PROFILES.ref_low_workspace = { magOut=.09, acquire=.26, magIn=.70, bolt=.84 }
RELOAD_EVENT_PROFILES.ref_muzzle_up_charge = { magOut=.11, acquire=.31, magIn=.72, bolt=.85, charge=true }
RELOAD_EVENT_PROFILES.ref_push_pull = { magOut=.10, acquire=.28, magIn=.68, tap=.75, bolt=.86 }
RELOAD_EVENT_PROFILES.ref_tactile_baseplate = { magOut=.10, acquire=.29, magIn=.69, tap=.77, bolt=.86 }
RELOAD_EVENT_PROFILES.ref_fast_emergency = { magOut=.075, acquire=.22, magIn=.65, bolt=.79 }
RELOAD_EVENT_PROFILES.ref_wide_pouch_draw = { magOut=.10, acquire=.30, magIn=.72, bolt=.86 }
RELOAD_EVENT_PROFILES.ref_palm_bolt_release = { magOut=.09, acquire=.27, magIn=.69, bolt=.83 }
RELOAD_EVENT_PROFILES.ref_overhand_charge = { magOut=.10, acquire=.30, magIn=.71, bolt=.84, charge=true }

-- V24 live-magazine carrier-retention variants. Every one stows the old
-- magazine before drawing the replacement; the contact timing is deliberately
-- imperfect and matches the visible correction in its animation.
RELOAD_EVENT_PROFILES.retain_shaky_index = { magOut=.19, acquire=.51, contact=.70, magIn=.80 }
RELOAD_EVENT_PROFILES.retain_front_lip = { magOut=.19, acquire=.50, contact=.69, magIn=.84 }
RELOAD_EVENT_PROFILES.retain_rear_lip = { magOut=.20, acquire=.51, contact=.70, magIn=.84 }
RELOAD_EVENT_PROFILES.retain_pouch_hesitation = { magOut=.19, acquire=.59, contact=.76, magIn=.85 }
RELOAD_EVENT_PROFILES.retain_uncertain_seat = { magOut=.19, acquire=.50, contact=.72, magIn=.79, tap=.89 }


-- V25 distinct tactical-retention cycle. Fresh-first profiles deliberately
-- acquire the loaded magazine before the partial magazine leaves the weapon.
RELOAD_EVENT_PROFILES.rifle_fresh_first_l_index = { acquire=.15, magOut=.43, contact=.66, magIn=.72 }
RELOAD_EVENT_PROFILES.rifle_parallel_palm_stack = { acquire=.16, magOut=.45, contact=.69, magIn=.75 }
RELOAD_EVENT_PROFILES.rifle_carrier_stow_front_lip = { magOut=.18, acquire=.55, contact=.72, magIn=.84 }
RELOAD_EVENT_PROFILES.rifle_reverse_l_index_snag = { acquire=.21, magOut=.47, contact=.71, magIn=.77 }
RELOAD_EVENT_PROFILES.rifle_retention_pull_check = { magOut=.18, acquire=.54, contact=.72, magIn=.81, tap=.90 }
RELOAD_EVENT_PROFILES.pistol_fresh_first_l_index = { acquire=.16, magOut=.43, contact=.66, magIn=.72 }
RELOAD_EVENT_PROFILES.pistol_parallel_palm_stack = { acquire=.17, magOut=.45, contact=.69, magIn=.74 }
RELOAD_EVENT_PROFILES.pistol_carrier_front_lip = { magOut=.18, acquire=.53, contact=.72, magIn=.82 }
RELOAD_EVENT_PROFILES.pistol_reverse_l_index_snag = { acquire=.22, magOut=.47, contact=.71, magIn=.76 }
RELOAD_EVENT_PROFILES.pistol_retention_pull_check = { magOut=.18, acquire=.52, contact=.71, magIn=.79, tap=.90 }


local MIN_CARRIER_RETAIN_ROUNDS = 5

local function reloadMagFed(_emergency: boolean)
	local w = W()
	if fracture("RightArm") >= 3 or fracture("LeftArm") >= 3 then
		Hud.status("ARM NONFUNCTIONAL — CANNOT RELOAD")
		return
	end
	local H = Cfg.HANDLING
	local roundsBeforeReload = w.mag.rounds
	local emptyMagazine = w.boltLocked or roundsBeforeReload <= 0
	-- A live magazine is never thrown on the floor. At five or more rounds it
	-- receives the full carrier-retention choreography requested by the player.
	local retainMagazine = not emptyMagazine and roundsBeforeReload >= MIN_CARRIER_RETAIN_ROUNDS
	local stow = retainMagazine and H.MAG_STOW or 0
	local release = emptyMagazine and H.BOLT_RELEASE or 0
	local total = (H.MAG_OUT + stow + H.MAG_IN + release)
		* armPenalty(Cfg.HEALTH.ARM_RELOAD_MAX)

	local token = beginAction("reload")
	local animName = emptyMagazine and "reload_empty" or "reload_tactical"
	local actualTotal, selectedName, selected = anim:play(animName, total)
	actualTotal = actualTotal or total
	selectedName = selectedName or animName

	local technique = selected and selected.technique or nil
	print(("[BLACKSITE] reload variant %s / %s"):format(selectedName, technique or "default"))
	local profile = RELOAD_EVENT_PROFILES[technique or ""]
		or RELOAD_EVENT_PROFILES[selectedName]
		or RELOAD_EVENT_PROFILES[animName]

	task.spawn(function()
		local started = os.clock()
		local pendingMag: Mag? = nil
		local removedMag = w.mag
		local oldRemoved = false
		local newInserted = false
		local boltFinished = not w.boltLocked

		local function waitFraction(fraction: number): boolean
			local target = actualTotal * math.clamp(fraction, 0, 1)
			local remaining = target - (os.clock() - started)
			if remaining <= 0 then
				return actionToken == token and alive
			end
			return hold(token, remaining)
		end

		local events = {
			{ at = profile.magOut or 0.16, kind = "magOut" },
			{ at = profile.acquire or 0.48, kind = "acquire" },
			{ at = profile.contact, kind = "contact" },
			{ at = profile.magIn or 0.78, kind = "magIn" },
			{ at = profile.tap, kind = "tap" },
			{ at = profile.bolt or 0.90, kind = "bolt" },
		}
		table.sort(events, function(a, b)
			return (a.at or math.huge) < (b.at or math.huge)
		end)

		for _, event in events do
			if event.at and not waitFraction(event.at) then
				return
			end

			if event.kind == "acquire" and not pendingMag then
				local idx = bestMag(w.rig)
				if idx then
					pendingMag = table.remove(w.rig, idx) :: Mag
					playLocal(Cfg.SFX.POUCH_DRAW, 0.04)
				end
			elseif event.kind == "magOut" and not oldRemoved then
				oldRemoved = true
				w.mag = { cap = w.def.magCap, rounds = 0 }
				playLocal(Cfg.SFX.MAG_OUT, 0.035)
				if retainMagazine then
					table.insert(w.rig, removedMag)
				else
					-- Only an actually empty magazine is discarded.
					dropMag()
				end
			elseif event.kind == "contact" then
				playLocal(Cfg.SFX.MAG_CONTACT, 0.04)
			elseif event.kind == "magIn" and not newInserted then
				if not pendingMag then
					local idx = bestMag(w.rig)
					if idx then
						pendingMag = table.remove(w.rig, idx) :: Mag
					end
				end
				if not pendingMag then
					w.mag = { cap = w.def.magCap, rounds = 0 }
					Hud.say("No magazines.", 3, Hud.COLORS.BAD)
				else
					w.mag = pendingMag
					newInserted = true
					playLocal(Cfg.SFX.MAG_IN, 0.025)
					if hrp then
						rNoise:FireServer((hrp :: BasePart).Position, Cfg.NOISE.RELOAD)
					end
				end
			elseif event.kind == "tap" and newInserted then
				playLocal(Cfg.SFX.MAG_TAP, 0.025)
			elseif event.kind == "bolt" and w.boltLocked and newInserted and not boltFinished then
				boltFinished = true
				playLocal(profile.charge and Cfg.SFX.CHARGING_HANDLE or Cfg.SFX.BOLT_RELEASE, 0.02)
				w.boltLocked = false
				if w.mag.rounds > 0 then
					w.mag.rounds -= 1
					w.chambered = true
				end
			end
		end

		if not waitFraction(1) then
			return
		end

		-- Fallback safety for unusual animation metadata.
		if not oldRemoved then
			w.mag = { cap = w.def.magCap, rounds = 0 }
			if retainMagazine then
				table.insert(w.rig, removedMag)
			else
				dropMag()
			end
		end
		if not newInserted and pendingMag then
			w.mag = pendingMag
			newInserted = true
		end
		if w.boltLocked and newInserted and not boltFinished then
			w.boltLocked = false
			if w.mag.rounds > 0 then
				w.mag.rounds -= 1
				w.chambered = true
			end
		end

		finish(token)
	end)
end

local function reloadTubeFed()
	local w = W()
	if w.reserve <= 0 then
		Hud.say("Out of shells.", 3, Hud.COLORS.BAD)
		return
	end
	local token = beginAction("shells")
	local shellDuration, _, selected = anim:play("shells_load", Cfg.HANDLING.SHELL_LOAD)
	shellDuration = shellDuration or Cfg.HANDLING.SHELL_LOAD
	local startDuration = Cfg.HANDLING.SHELL_START * (selected and selected.startScale or 1)

	task.spawn(function()
		if not hold(token, startDuration) then
			return
		end

		local first = true
		while w.mag.rounds < w.mag.cap and w.reserve > 0 do
			if not first then
				-- Re-resolve for every shell. The support hand alternates between
				-- centreline, strong-side and rolled loading paths instead of
				-- looping one identical motion for the whole tube.
				shellDuration = anim:play("shells_load", Cfg.HANDLING.SHELL_LOAD) or Cfg.HANDLING.SHELL_LOAD
			end
			first = false

			local stamp = actionToken
			task.delay(shellDuration * 0.72, function()
				if actionToken == stamp and alive then
					playLocal(Cfg.SFX.SHELL_IN, 0.035)
				end
			end)
			if not hold(token, shellDuration) then
				return
			end
			w.mag.rounds += 1
			w.reserve -= 1
		end

		if not w.chambered and w.mag.rounds > 0 then
			playLocal(Cfg.SFX.CHARGING_HANDLE, 0.02)
			if not hold(token, Cfg.HANDLING.BOLT_RELEASE) then
				return
			end
			w.mag.rounds -= 1
			w.chambered = true
		end
		finish(token)
	end)
end

local function doReload(emergency: boolean)
	if busy or not alive or st.sprinting then
		return
	end
	local w = W()
	if w.def.feed == "tube" then
		reloadTubeFed()
	else
		if w.mag.rounds >= w.mag.cap and w.chambered then
			Hud.say("Topped off.", 1.6)
			return
		end
		-- Carrier retention starts at five live rounds. With one to four rounds
		-- remaining, the magazine stays in the rifle instead of being discarded.
		if not w.boltLocked and w.mag.rounds > 0 and w.mag.rounds < MIN_CARRIER_RETAIN_ROUNDS then
			Hud.say("Run it dry, or retain it with 5+ rounds.", 2.0)
			return
		end
		reloadMagFed(emergency)
	end
end

local function checkMag()
	if busy or not alive then
		return
	end
	local w = W()
	local token = beginAction("checkmag")
	anim:play("check_mag", Cfg.HANDLING.CHECK_MAG)
	task.spawn(function()
		playLocal(Cfg.SFX.MAG_OUT, 0.05)
		if not hold(token, Cfg.HANDLING.CHECK_MAG) then
			return
		end
		playLocal(Cfg.SFX.MAG_IN, 0.05)

		if w.def.feed == "tube" then
			Hud.say(
				("%s in the tube -- %d loose on the rig."):format(
					Weapons.describeMag(w.mag.rounds, w.mag.cap),
					w.reserve
				),
				3
			)
		else
			Hud.say(
				("%s -- %s."):format(
					Weapons.describeMag(w.mag.rounds, w.mag.cap),
					Weapons.describeRig(#w.rig)
				),
				3
			)
		end
		finish(token)
	end)
end

local function checkChamber()
	if busy or not alive then
		return
	end
	local w = W()
	local token = beginAction("checkchamber")
	anim:play("check_chamber", Cfg.HANDLING.CHECK_CHAMBER)
	task.spawn(function()
		playLocal(Cfg.SFX.BOLT, 0.15)
		if not hold(token, Cfg.HANDLING.CHECK_CHAMBER) then
			return
		end
		if w.boltLocked then
			Hud.say("Bolt's locked back.", 2.4, Hud.COLORS.WARN)
		elseif w.chambered then
			Hud.say("Brass in the chamber.", 2.4, Hud.COLORS.GOOD)
		else
			Hud.say("Chamber's empty.", 2.4, Hud.COLORS.BAD)
		end
		finish(token)
	end)
end

local function cycleSelector()
	if busy or not alive then
		return
	end
	local w = W()
	w.modeIndex = w.modeIndex % #w.def.fireModes + 1
	st.burstLeft = 0
	if anim then
		anim:play("fire_selector", Cfg.HANDLING.FIRE_SELECTOR)
	end
	playLocal(Cfg.SFX.SELECTOR)
	Hud.status(("%s // %s"):format(w.def.name, mode()))
end

local function swapTo(slot: string)
	if busy or not alive or slot == currentSlot or not slots[slot] then
		return
	end
	local token = beginAction("swap")
	local half = Cfg.HANDLING.SWAP_WEAPON * 0.45
	task.spawn(function()
		-- First half puts the old weapon away (the LOW_READY pose does that),
		-- second half brings the new one up on its own timeline.
		playLocal(Cfg.SFX.SWAP, 0.05)
		if not hold(token, half) then
			return
		end
		currentSlot = slot
		buildViewmodel()
		anim:play("swap", Cfg.HANDLING.SWAP_WEAPON - half)
		if not hold(token, Cfg.HANDLING.SWAP_WEAPON - half) then
			return
		end
		Hud.status(("%s // %s"):format(W().def.name, mode()))
		finish(token)
	end)
end

local function toggleSuppressor()
	if busy or not alive then
		return
	end
	local w = W()
	if not w.def.suppressible then
		Hud.say("Nothing to thread it onto.", 2)
		return
	end
	local token = beginAction("can")
	task.spawn(function()
		if not hold(token, 1.5) then
			return
		end
		w.suppressed = not w.suppressed
		buildViewmodel()
		Hud.status(w.suppressed and "SUPPRESSED" or "MUZZLE BARE")
		finish(token)
	end)
end

local function inspect()
	if busy or not alive then
		return
	end
	local token = beginAction("inspect")
	st.inspecting = true
	anim:play("inspect", Cfg.HANDLING.INSPECT)
	task.spawn(function()
		local ok = hold(token, Cfg.HANDLING.INSPECT)
		st.inspecting = false
		if ok then
			finish(token)
		end
	end)
end

local function bandage()
	if busy or not alive then
		return
	end
	if (player:GetAttribute("Bandages") or 0) <= 0 then
		Hud.say("No dressings left.", 2.5, Hud.COLORS.BAD)
		return
	end
	local token = beginAction("bandage")
	task.spawn(function()
		Hud.say("Packing it...", Cfg.HANDLING.BANDAGE)
		if not hold(token, Cfg.HANDLING.BANDAGE) then
			Hud.say("Interrupted.", 2, Hud.COLORS.WARN)
			return
		end
		rBandage:FireServer()
		finish(token)
	end)
end

--------------------------------------------------------------------------------
-- Interaction
--------------------------------------------------------------------------------
local interactTarget: BasePart? = nil

local PROMPTS = {
	FUSE = "[F]  TAKE POWER CELL",
	GENERATOR = "[F]  SEAT CELLS",
	EXIT = "[F]  CALL LIFT",
	DOOR = "[F]  DOOR",
}

local function updateInteract()
	interactTarget = nil
	if not alive or not character then
		Hud.prompt(nil)
		return
	end
	local params = RaycastParams.new()
	params.FilterType = Enum.RaycastFilterType.Exclude
	params.FilterDescendantsInstances = ignoreList()
	local cam = camera().CFrame
	local res = workspace:Raycast(cam.Position, cam.LookVector * 11, params)
	if res then
		local tag = res.Instance:GetAttribute("Interact")
		if tag then
			interactTarget = res.Instance
			Hud.prompt(PROMPTS[tag] or "[F]  USE")
			return
		end
	end
	Hud.prompt(nil)
end

--------------------------------------------------------------------------------
-- Input
--   Q / E    lean            F   interact
--   L        weapon light    K   laser
--   R        reload (hold to dump the mag)
--   T / Y    mag check / chamber check
--   X        fire selector   V   inspect    B   suppressor
--------------------------------------------------------------------------------
local reloadPending = false
local reloadPress = 0

UserInputService.InputBegan:Connect(function(input, processed)
	if processed then
		return
	end

	if input.UserInputType == Enum.UserInputType.MouseButton1 then
		st.triggerDown = true
		st.firedThisPull = false
		if mode() == "BURST" then
			st.burstLeft = W().def.burstCount
		end
		return
	elseif input.UserInputType == Enum.UserInputType.MouseButton2 then
		st.adsHeld = true
		return
	end

	local k = input.KeyCode
	if k == Enum.KeyCode.LeftShift then
		if st.adsHeld then
			st.holdBreathHeld = true
		elseif st.stamina >= Cfg.MOVE.STAMINA_SPRINT_MIN and not st.crouched and canSprintWithLegs() then
			st.sprinting = true
			if busy then
				cancelAction() -- you cannot reload at a dead run
			end
		end
	elseif k == Enum.KeyCode.LeftControl then
		st.slowWalk = true
	elseif k == Enum.KeyCode.C then
		-- ANALOG STANCE. Hold C and the mouse sets your exact height, so you
		-- can put your eye just over the lip of a crate instead of choosing
		-- between two poses. Tap it and you get the old toggle.
		st.analogMode = "crouch"
		st.analogSince = os.clock()
		st.analogMoved = false
		st.sprinting = false
	elseif k == Enum.KeyCode.Q or k == Enum.KeyCode.E then
		-- HOLD to lean, release to come back up. The mouse still trims the exact
		-- angle while you are holding, so you keep the analog control without
		-- having to remember which way you left yourself parked.
		local side = (k == Enum.KeyCode.Q) and -1 or 1
		st.analogMode = "lean"
		st.analogSide = side
		st.analogSince = os.clock()
		st.analogMoved = false
		st.leanTarget = side
		inertiaPosVel += Vector3.new(-side * Cfg.STANCE.LEAN_IMPULSE, 0, 0)
	elseif k == Enum.KeyCode.G then
		-- Low ready. Holding a rifle up is work -- this is how you rest, and
		-- the only reason arm stamina is a mechanic rather than a punishment.
		st.readyDown = not st.readyDown
		inertiaPosVel += Vector3.new(0, st.readyDown and -0.7 or 0.5, 0)
		Hud.status(st.readyDown and "LOW READY" or "WEAPON UP")
	elseif k == Enum.KeyCode.F then
		if interactTarget then
			rInteract:FireServer(interactTarget)
		end
	elseif k == Enum.KeyCode.LeftAlt then
		st.freeLook = true
	elseif k == Enum.KeyCode.R then
		reloadPending = true
		reloadPress = os.clock()
		local stamp = reloadPress
		-- Held past the threshold: emergency reload fires immediately, so the
		-- speed you are paying for is real rather than waiting on key release.
		task.delay(Cfg.HANDLING.EMERGENCY_HOLD, function()
			if reloadPending and reloadPress == stamp and UserInputService:IsKeyDown(Enum.KeyCode.R) then
				reloadPending = false
				doReload(true)
			end
		end)
	elseif k == Enum.KeyCode.T then
		checkMag()
	elseif k == Enum.KeyCode.Y then
		checkChamber()
	elseif k == Enum.KeyCode.X then
		cycleSelector()
	elseif k == Enum.KeyCode.V then
		inspect()
	elseif k == Enum.KeyCode.B then
		toggleSuppressor()
	elseif k == Enum.KeyCode.One then
		swapTo("primary")
	elseif k == Enum.KeyCode.Two then
		swapTo("sidearm")
	elseif k == Enum.KeyCode.L then
		st.torch = not st.torch
		rGear:FireServer("torch", st.torch)
		Hud.status(st.torch and "LIGHT ON -- you are the brightest thing here" or "LIGHT OFF")
	elseif k == Enum.KeyCode.K then
		st.laser = not st.laser
		rGear:FireServer("laser", st.laser)
		Hud.status(st.laser and "LASER ON" or "LASER OFF")
	elseif k == Enum.KeyCode.H then
		bandage()
	elseif k == Enum.KeyCode.Space then
		if humanoid and not st.crouched then
			(humanoid :: Humanoid).Jump = true
		end
	elseif k == Enum.KeyCode.F3 then
		-- INSTRUMENTATION, not a feature. Three rounds of "the effects are still
		-- garbage" after tripling every constant twice means guessing at numbers
		-- is worthless -- what matters is whether the pulses arrive at all. If
		-- these stay at 0.00 while you shoot, the events are not firing and no
		-- constant will ever fix it. If they spike and the screen still does
		-- nothing, the fault is downstream in Atmosphere applying them.
		debugFx = not debugFx
		Hud.status(debugFx and "FX DEBUG ON -- F3 to hide" or "FX DEBUG OFF")
	elseif k == Enum.KeyCode.F1 then
		Hud.toggleHelp()
	end
end)

UserInputService.InputEnded:Connect(function(input)
	if input.UserInputType == Enum.UserInputType.MouseButton1 then
		st.triggerDown = false
		st.burstLeft = 0
		return
	elseif input.UserInputType == Enum.UserInputType.MouseButton2 then
		st.adsHeld = false
		st.holdBreathHeld = false
		return
	end

	local k = input.KeyCode
	if k == Enum.KeyCode.LeftShift then
		st.sprinting = false
		st.holdBreathHeld = false
	elseif k == Enum.KeyCode.LeftControl then
		st.slowWalk = false
	elseif k == Enum.KeyCode.C then
		if st.analogMode == "crouch" then
			st.analogMode = nil
			-- A tap toggles; a hold leaves you wherever you steered to.
			if not st.analogMoved and os.clock() - st.analogSince < Cfg.STANCE.TAP_TIME then
				st.stanceTarget = st.stanceTarget > 0.5 and 0 or 1
			end
			if st.stanceTarget < Cfg.STANCE.ANALOG_MIN then
				st.stanceTarget = 0
			end
			-- Your knees giving throws the weapon down; standing lifts it.
			inertiaPosVel += Vector3.new(0, (st.stanceTarget > 0.5 and -1 or 0.6) * Cfg.STANCE.CROUCH_IMPULSE, 0)
		end
	elseif k == Enum.KeyCode.Q or k == Enum.KeyCode.E then
		local side = (k == Enum.KeyCode.Q) and -1 or 1
		-- Only the key you are actually leaning on releases the lean, so
		-- tapping the opposite key mid-lean does not leave you stuck over.
		if st.analogSide == side or st.leanTarget * side > 0 then
			if st.analogMode == "lean" then
				st.analogMode = nil
			end
			inertiaPosVel += Vector3.new(st.leanTarget * Cfg.STANCE.LEAN_IMPULSE * 0.7, 0, 0)
			st.leanTarget = 0
		end
	elseif k == Enum.KeyCode.R then
		if reloadPending then
			reloadPending = false
			doReload(false) -- released quickly: tactical, keep the partial mag
		end
	end
end)

--------------------------------------------------------------------------------
-- Movement. The default control module is switched off so that free look does
-- not steer the character -- Roblox's controls move relative to the camera.
--------------------------------------------------------------------------------
local controlsDisabled = false
local function disableDefaultControls()
	if controlsDisabled then
		return
	end
	local ok = pcall(function()
		local pm = require(player:WaitForChild("PlayerScripts"):WaitForChild("PlayerModule")) :: any
		pm:GetControls():Disable()
	end)
	controlsDisabled = ok
	if not ok then
		warn("[BLACKSITE] Could not disable PlayerModule controls; free look will steer movement.")
	end
end

local function moveInput(): Vector3
	local v = Vector3.zero
	if UserInputService:IsKeyDown(Enum.KeyCode.W) then
		v += Vector3.new(0, 0, -1)
	end
	if UserInputService:IsKeyDown(Enum.KeyCode.S) then
		v += Vector3.new(0, 0, 1)
	end
	if UserInputService:IsKeyDown(Enum.KeyCode.A) then
		v += Vector3.new(-1, 0, 0)
	end
	if UserInputService:IsKeyDown(Enum.KeyCode.D) then
		v += Vector3.new(1, 0, 0)
	end
	return v.Magnitude > 0 and v.Unit or v
end

local function currentSpeed(): number
	local base
	if st.crouched then
		base = Cfg.MOVE.CROUCH
	elseif st.sprinting then
		base = Cfg.MOVE.SPRINT
	elseif st.slowWalk then
		base = Cfg.MOVE.SLOW
	elseif st.ads then
		base = Cfg.MOVE.ADS
	else
		base = Cfg.MOVE.WALK
	end
	return base * legMobility()
end

local function stanceName(moving: boolean): string
	if not moving then
		return "still"
	elseif st.sprinting then
		return "sprint"
	elseif st.crouched then
		return "crouch"
	elseif st.slowWalk then
		return "slow"
	end
	return "walk"
end

--------------------------------------------------------------------------------
-- Character lifecycle
--------------------------------------------------------------------------------
local function onCharacter(char: Model)
	clearFirstPersonBody()
	character = char
	local hum = char:WaitForChild("Humanoid") :: Humanoid
	local root = char:WaitForChild("HumanoidRootPart") :: BasePart
	humanoid = hum
	hrp = root
	alive = true

	hum.AutoRotate = false
	hum.CameraOffset = Vector3.zero

	bodyParts = {} -- V26 visibility is owned by FirstPersonBody on the actual R15 rig.

	-- The whole chain starts aligned, so nothing springs on spawn.
	local yaw0 = select(2, root.CFrame:ToEulerAnglesYXZ())
	st.aimYaw, st.aimPitch = yaw0, 0
	st.headYaw, st.headPitch, st.headYawVel, st.headPitchVel = yaw0, 0, 0, 0
	st.headRoll, st.headRollVel = 0, 0
	st.cameraEyePos, st.cameraEyeVel = nil, Vector3.zero
	st.torsoYaw, st.torsoPitch, st.torsoYawVel, st.torsoPitchVel = yaw0, 0, 0, 0
	st.hipYaw, st.hipYawVel = yaw0, 0

	st.crouched = false
	st.sprinting = false
	st.adsHeld = false
	st.ads = false
	st.torch = false
	st.laser = false
	st.readyDown = false
	st.analogMode = nil
	st.stance, st.stanceVel, st.stanceTarget = 0, 0, 0
	st.lean, st.leanVel, st.leanTarget = 0, 0, 0
	st.fatigue, st.sag, st.sagVel = 0, 0, 0
	st.collide = 0
	st.weaponHeat = 0
	st.stamina = Cfg.MOVE.STAMINA_MAX
	st.breath = Cfg.CAM.BREATH_MAX
	cancelAction()

	carryPos, carryRot = splitPose(Cfg.VIEWMODEL.HIP)
	carryPosVel, carryRotVel = Vector3.zero, Vector3.zero
	carryCF = Cfg.VIEWMODEL.HIP
	stanceCF = Cfg.VIEWMODEL.HIP
	inertiaRot, inertiaRotVel = Vector3.zero, Vector3.zero
	inertiaPos, inertiaPosVel = Vector3.zero, Vector3.zero
	prevVelLocal, bodyAccel = Vector3.zero, Vector3.zero
	adsVel = 0
	landDip, landDipVel = 0, 0
	stepDip, stepDipVel = 0, 0
	stepRoll, stepRollVel = 0, 0
	strafeRoll, strafeRollVel = 0, 0
	st.recoilRise, st.recoilRiseVel = 0, 0
	st.recoilSide, st.recoilSideVel = 0, 0
	st.recoilBack, st.recoilBackVel = 0, 0
	prevAds = false
	st.adsAlpha = 0
	st.hipFov = Cfg.CAM.FOV_HIP

	-- Landing drives a much bigger impulse than a footfall. This is the one
	-- piece of weapon motion the player causes without touching the mouse.
	hum.StateChanged:Connect(function(_, newState)
		if newState == Enum.HumanoidStateType.Landed then
			inertiaPosVel += Vector3.new(0, -Cfg.INERTIA.LAND_IMPULSE, 0)
			-- The head drops too, not just the gun. Seeding the spring's
			-- VELOCITY rather than moving its target is the whole difference
			-- between being hit and being filtered.
			landDipVel += Cfg.CAMSHAKE.LAND_DIP * math.sqrt(Cfg.CAMSHAKE.LAND_K)
			st.torsoPitchVel += 0.9
		end
	end)

	slots.primary = newWeapon("MK18")
	slots.sidearm = newWeapon("P320")
	currentSlot = "primary"

	disableDefaultControls()
	buildViewmodel()
	-- The local player now owns the same articulated body used in third person.
	-- Its Head drives the camera and its real arms are posed onto the weapon.
	fpBodyRig = SoldierVisual.build(char)
	Hud.fade(0, 1.2)
	Hud.status(("%s // %s"):format(W().def.name, mode()))
	Hud.say(
		Cfg.MODE == "ARENA" and "Arena. Cover's every height that matters -- hold C and Q/E."
			or "Insertion complete. Facility's dark.",
		5
	)

	hum.Died:Connect(function()
		alive = false
		destroyAllViewmodels()
		clearFirstPersonBody()
		Hud.fade(1, 1.5)
		Hud.prompt(nil)
		Hud.say("", 0.1)
	end)
end

if player.Character then
	onCharacter(player.Character)
end
player.CharacterAdded:Connect(onCharacter)

--------------------------------------------------------------------------------
-- Server -> client
--------------------------------------------------------------------------------
rMessage.OnClientEvent:Connect(function(text, duration)
	Hud.say(text, duration)
end)

rEffect.OnClientEvent:Connect(function(kind, a, b, c)
	if kind == "hurt" then
		Hud.hitFlash()
		Hud.hitLimb(typeof(b) == "string" and b or nil)
		Fx.pulse("hit", 1)
		st.headYawVel += rng:NextNumber(-1, 1) * Cfg.SCREENFX.HIT_SHAKE
		st.headPitchVel += rng:NextNumber(-1, 1) * Cfg.SCREENFX.HIT_SHAKE
	elseif kind == "suppress" then
		-- A round cracked past. `a` is 0..1 by how close it was. It shoves the
		-- head spring as well as blurring, so you flinch -- which is the part
		-- that makes incoming fire actually frightening rather than a filter.
		local close = typeof(a) == "number" and a or 1
		local FX = Cfg.SCREENFX
		Fx.pulse("suppress", close)
		-- The crack is the projectile reaching the listener; the distant muzzle
		-- report is a separate event. Keeping them separate is essential for
		-- believable incoming fire.
		playLocal(close > 0.62 and Cfg.SFX.BULLET_CRACK or Cfg.SFX.BULLET_WHIZZ, 0.035)
		st.headYawVel += rng:NextNumber(-1, 1) * FX.SUPPRESS_SHAKE * close
		st.headPitchVel += rng:NextNumber(-0.5, 1) * FX.SUPPRESS_SHAKE * close
		-- The weapon gets jolted too, not just the view -- otherwise the flinch
		-- reads as a camera shake rather than as a person ducking.
		inertiaRotVel += Vector3.new(
			rng:NextNumber(-1, 1),
			rng:NextNumber(-1, 1),
			rng:NextNumber(-1, 1)
		) * FX.SUPPRESS_WEAPON_KICK * close
	elseif kind == "extract" then
		Hud.say("Lift's moving. You're out.", 6, Hud.COLORS.GOOD)
		Hud.fade(1, 3)
	elseif kind == "shot" then
		-- Other players use the same grounded mix, positioned at the muzzle. The
		-- mixer applies distance rolloff, obstruction filtering and a room tail.
		local def = c and Cfg.SFX.SHOT_SUPPRESSED or Cfg.SFX[b]
		if typeof(a) == "Vector3" and def then
			Audio.playWorld(def, a, 0.035, audioExclude())
		end
	end
end)

--------------------------------------------------------------------------------
-- Render loop
--------------------------------------------------------------------------------
local syncAccum = 0
local breathPhase = 0

-- An error thrown inside BindToRenderStep kills the REST of the callback every
-- frame, silently. Everything below the throw -- camera, weapon, free aim,
-- inertia -- simply never runs, and the symptom is "your changes did nothing".
-- This reports the first one loudly and then once a second instead of spamming.
local lastErrReport = 0
local function reportRenderError(err: string)
	local now = os.clock()
	if now - lastErrReport < 1 then
		return
	end
	lastErrReport = now
	warn(("[BLACKSITE] RENDER LOOP ERROR (%s) -- weapon feel is dead until this is fixed:\n%s")
		:format(BUILD, tostring(err)))
end

local function renderStep(dt: number)
	local cam = camera()
	cam.CameraType = Enum.CameraType.Scriptable
	UserInputService.MouseBehavior = Enum.MouseBehavior.LockCenter
	UserInputService.MouseIconEnabled = false

	if not alive or not hrp or not humanoid then
		return
	end
	local root = hrp :: BasePart
	local hum = humanoid :: Humanoid

	-- The viewmodel lives under the camera; if the camera was swapped out we
	-- lost it and have to rebuild.
	if rig and rig.folder.Parent ~= cam then
		rig.folder.Parent = cam
	end

	-- Evaluate the authored action once per frame. Its camera channel is consumed
	-- by the head chain below; its weapon/hand/bone channels are consumed by the
	-- viewmodel draw at the end. Updating once prevents camera and hands from
	-- drifting onto different timeline samples.
	local actionPose = nil
	if rig and anim then
		anim.boltLocked = W().boltLocked
		actionPose = anim:update(dt)
	end

	----------------------------------------------------------------------------
	-- Look
	----------------------------------------------------------------------------
	local B = Cfg.BODY
	local delta = UserInputService:GetMouseDelta()
	local sens = Cfg.CAM.SENS * (st.ads and Cfg.CAM.SENS_ADS_SCALE or 1)
	local dYaw = -delta.X * sens
	local dPitch = -delta.Y * sens

	----------------------------------------------------------------------------
	-- ANALOG STANCE. While Q/E/C is held the mouse steers your BODY, not your
	-- aim -- exact lean angle, exact crouch height. That is the trade, and it
	-- is why you can hold an angle over a crate instead of picking one of two
	-- canned poses. Aim is frozen for as long as you are steering.
	----------------------------------------------------------------------------
	if Cfg.STANCE.ANALOG_LEAN and st.analogMode == "lean" then
		if math.abs(delta.X) > 0.5 then
			st.analogMoved = true
		end
		st.leanTarget = math.clamp(st.leanTarget + delta.X * Cfg.STANCE.LEAN_PER_PIXEL, -1, 1)
		dYaw, dPitch = 0, 0
	elseif Cfg.STANCE.ANALOG_CROUCH and st.analogMode == "crouch" then
		if math.abs(delta.Y) > 0.5 then
			st.analogMoved = true
		end
		st.stanceTarget = math.clamp(st.stanceTarget + delta.Y * Cfg.STANCE.CROUCH_PER_PIXEL, 0, 1)
		dYaw, dPitch = 0, 0
	end

	if st.freeLook then
		st.freeYaw = math.clamp(st.freeYaw + dYaw, -Cfg.CAM.FREELOOK_YAW, Cfg.CAM.FREELOOK_YAW)
		st.freePitch = math.clamp(st.freePitch + dPitch, -Cfg.CAM.FREELOOK_PITCH, Cfg.CAM.FREELOOK_PITCH)
	else
		------------------------------------------------------------------------
		-- THE BORE. The mouse writes here and nowhere else, 1:1, no filtering,
		-- no clamping, no latency. Rounds go where this points. Every lagged
		-- thing below is dragged along BY it and none of it can cost you aim,
		-- which is the property that makes a heavy camera acceptable at all.
		------------------------------------------------------------------------
		st.aimYaw += dYaw
		st.aimPitch = math.clamp(st.aimPitch + dPitch, -Cfg.CAM.PITCH_LIMIT, Cfg.CAM.PITCH_LIMIT)

		st.freeYaw = lerpNum(st.freeYaw, 0, dt * 10)
		st.freePitch = lerpNum(st.freePitch, 0, dt * 10)
	end

	----------------------------------------------------------------------------
	-- THE CHAIN: bore -> head -> torso -> hips.
	--
	-- Each joint has slack then stiffness. Aiming collapses the slack and
	-- stiffens the springs, so ADS and hip fire are two different machines.
	----------------------------------------------------------------------------
	local a = math.clamp(st.adsAlpha, 0, 1)
	local dzScale = 1 - a * (1 - B.ADS_DZ_SCALE)
	local kScale = 1 + a * (B.ADS_STIFFEN - 1)
	local dScale = math.sqrt(kScale) -- keep the damping ratio where it was

	local headYawAccel, headPitchAccel
	st.headYaw, st.headYawVel, headYawAccel = jointStep(
		st.headYaw, st.headYawVel, st.aimYaw,
		B.HEAD_DZ_X * dzScale, B.HEAD_KNEE * dzScale,
		B.HEAD_K * kScale, B.HEAD_D * dScale, B.HEAD_MAX, dt
	)
	st.headPitch, st.headPitchVel, headPitchAccel = jointStep(
		st.headPitch, st.headPitchVel, st.aimPitch,
		B.HEAD_DZ_Y * dzScale, B.HEAD_KNEE * dzScale,
		B.HEAD_K * kScale, B.HEAD_D * dScale, B.HEAD_MAX, dt
	)
	st.headPitch = math.clamp(st.headPitch, -Cfg.CAM.PITCH_LIMIT, Cfg.CAM.PITCH_LIMIT)

	st.torsoYaw, st.torsoYawVel = jointStep(
		st.torsoYaw, st.torsoYawVel, st.headYaw,
		B.TORSO_DZ_X * dzScale, B.TORSO_KNEE * dzScale,
		B.TORSO_K * kScale, B.TORSO_D * dScale, B.TORSO_MAX, dt
	)
	st.torsoPitch, st.torsoPitchVel = jointStep(
		st.torsoPitch, st.torsoPitchVel, st.headPitch * B.TORSO_PITCH_SCALE,
		B.TORSO_DZ_Y * dzScale, B.TORSO_KNEE * dzScale,
		B.TORSO_K * kScale, B.TORSO_D * dScale, B.TORSO_MAX, dt
	)

	local invDt = 1 / math.max(dt, 1e-4)
	local headYawRate = st.headYawVel

	-- Vestibular roll: whip your view and your head banks into the turn and
	-- comes back. Two and a half degrees, and its absence is a large part of
	-- why a first-person camera reads as a tripod.
	local rollTarget = math.clamp(
		-headYawRate * B.VESTIBULAR_ROLL,
		-B.VESTIBULAR_MAX,
		B.VESTIBULAR_MAX
	)
	st.headRoll, st.headRollVel =
		springScalar(st.headRoll, st.headRollVel, rollTarget, B.VESTIBULAR_K, B.VESTIBULAR_D, dt)

	----------------------------------------------------------------------------
	-- Movement and stamina
	----------------------------------------------------------------------------
	local input = moveInput()
	local moving = input.Magnitude > 0

	if st.sprinting and (not moving or st.stamina <= 0 or st.crouched or not canSprintWithLegs()) then
		st.sprinting = false
	end
	if st.sprinting then
		st.stamina = math.max(0, st.stamina - dt)
	else
		st.stamina = math.min(Cfg.MOVE.STAMINA_MAX, st.stamina + dt * Cfg.MOVE.STAMINA_REGEN)
	end

	-- Working the pump on a shotgun keeps the gun in the shoulder; a reload or a
	-- mag check does not. Only the latter should break your sight picture.
	local handsBusy = busy and busyLabel ~= "pump"

	-- Aiming is resolved here rather than on the input event, so that letting
	-- go of sprint or stepping back off a wall puts you back in the sights.
	st.ads = st.adsHeld and not st.sprinting and not st.highPort and not handsBusy
	st.holdingBreath = st.holdBreathHeld and st.ads and st.breath > 0

	----------------------------------------------------------------------------
	-- HIPS. They chase the chest through a very loose joint, so you can look
	-- well over your shoulder before your feet come round -- and turning while
	-- running makes you arc instead of pivoting on the spot. While you are
	-- actually walking there is an extra alignment pull, because a walking
	-- person squares up and a standing one does not.
	----------------------------------------------------------------------------
	st.hipYaw, st.hipYawVel = jointStep(
		st.hipYaw, st.hipYawVel, st.torsoYaw,
		B.HIPS_DZ, B.HIPS_KNEE,
		B.HIPS_K + (moving and B.HIPS_WALK_ALIGN or 0), B.HIPS_D,
		B.HIPS_MAX, dt
	)

	hum.WalkSpeed = currentSpeed()

	----------------------------------------------------------------------------
	-- MOVEMENT GOES WHERE YOU ARE LOOKING.
	--
	-- This used to be driven off hipYaw, and the hips lag the head through a
	-- deadzone -- so pressing W sent you up to 34 degrees away from where you
	-- were pointing, and the harder you turned mid-run the further off you went.
	-- That is a fine model for a visible body and a terrible one for a control
	-- scheme: the player's own input has to be honoured exactly, the same way
	-- the bore is.
	--
	-- So the movement basis is the HEAD, and the hips are now purely cosmetic --
	-- they rotate the character model, which in first person nobody can see.
	local moveCF = CFrame.new(root.Position) * CFrame.Angles(0, st.headYaw, 0)
	local bodyCF = moveCF -- velocity/strafe maths wants the view frame too
	hum:Move(moving and moveCF:VectorToWorldSpace(input) or Vector3.zero, false)
	root.CFrame = CFrame.new(root.Position) * CFrame.Angles(0, st.hipYaw, 0)

	----------------------------------------------------------------------------
	-- Stance, lean, breath
	----------------------------------------------------------------------------
	-- Sprinting stands you up and squares you off, whatever you had steered to.
	local stanceGoal = st.sprinting and 0 or st.stanceTarget
	st.stance, st.stanceVel =
		springScalar(st.stance, st.stanceVel, stanceGoal, Cfg.STANCE.CROUCH_K, Cfg.STANCE.CROUCH_D, dt)
	st.crouched = st.stance > 0.5
	local eyeHeight = lerpNum(Cfg.MOVE.EYE_HEIGHT_STAND, Cfg.MOVE.EYE_HEIGHT_CROUCH, math.clamp(st.stance, 0, 1))

	local leanGoal = st.sprinting and 0 or st.leanTarget
	if leanGoal ~= 0 then
		-- Do not lean into a wall. Continuous now: the wall gives you as much
		-- lean as there is room for instead of refusing the whole movement, so
		-- you can slice a doorway right up to the frame.
		local params = RaycastParams.new()
		params.FilterType = Enum.RaycastFilterType.Exclude
		params.FilterDescendantsInstances = ignoreList()
		local eyePos = root.Position + Vector3.new(0, eyeHeight, 0)
		local want = Cfg.MOVE.LEAN_OFFSET * math.abs(leanGoal) + 0.9
		local side = leanGoal < 0 and -1 or 1
		local hit = workspace:Raycast(eyePos, bodyCF.RightVector * side * want, params)
		if hit then
			local room = math.max(0, (hit.Position - eyePos).Magnitude - 0.9)
			leanGoal = math.clamp(room / Cfg.MOVE.LEAN_OFFSET, 0, 1) * side
		end
	end
	st.lean, st.leanVel =
		springScalar(st.lean, st.leanVel, leanGoal, Cfg.STANCE.LEAN_K, Cfg.STANCE.LEAN_D, dt)

	----------------------------------------------------------------------------
	-- ARM STAMINA. The gun is up if you are neither at low ready nor sprinting.
	-- Fatigue buys sway and a slower presentation, and the low ready buys it
	-- back -- which is the entire reason OPERATOR has a ready-position system
	-- and why this build previously had the poses and no reason to use them.
	----------------------------------------------------------------------------
	local AS = Cfg.ARM_STAMINA
	local weaponUp = not st.readyDown and not st.sprinting
	if st.sprinting then
		st.fatigue -= AS.RECOVER_SPRINT * dt
	elseif st.readyDown then
		st.fatigue -= AS.RECOVER_READY * dt
	elseif st.ads then
		st.fatigue += AS.DRAIN_ADS * dt
	else
		st.fatigue += AS.DRAIN_HIP * dt
	end
	st.fatigue = math.clamp(st.fatigue, 0, 1)
	st.sag, st.sagVel = springScalar(
		st.sag, st.sagVel,
		weaponUp and st.fatigue * AS.SAG * (1 - a * 0.55) or 0,
		AS.SAG_K, AS.SAG_D, dt
	)

	if st.holdingBreath then
		st.breath = math.max(0, st.breath - dt)
	else
		st.breath = math.min(
			Cfg.CAM.BREATH_MAX,
			st.breath + dt * Cfg.CAM.BREATH_RECOVER * Cfg.CAM.BREATH_MAX / 3
		)
	end

	-- Everything shakes more when you are out of breath.
	local exertion = 1 + (1 - st.stamina / Cfg.MOVE.STAMINA_MAX) * 1.8
	breathPhase += dt * Cfg.CAM.BREATH_SPEED * exertion

	local breathAmp
	if st.holdingBreath then
		breathAmp = Cfg.CAM.BREATH_AMPLITUDE_HELD
	elseif st.ads then
		breathAmp = Cfg.CAM.BREATH_AMPLITUDE_ADS
	else
		breathAmp = Cfg.CAM.BREATH_AMPLITUDE_HIP
	end
	breathAmp *= exertion * (2 - W().def.ergonomics)
	local breathYaw = math.sin(breathPhase * 0.77) * breathAmp
	local breathPitch = math.sin(breathPhase * 1.31 + 1.2) * breathAmp * 0.7

	----------------------------------------------------------------------------
	-- Recoil recovery, bloom decay
	--
	-- Three springs on the WEAPON, turning about the shoulder pocket, rated off
	-- the weapon's own recoilRecover. The camera is not involved: it gets its
	-- share through the head and torso joints, which were kicked at the shot.
	----------------------------------------------------------------------------
	local def = W().def
	local RC = Cfg.RECOIL
	local recD = 2 * math.sqrt(RC.K) * RC.DAMP * (def.recoilRecover / RC.RECOVER_REF)
	st.recoilRise, st.recoilRiseVel = springScalar(st.recoilRise, st.recoilRiseVel, 0, RC.K, recD, dt)
	st.recoilSide, st.recoilSideVel = springScalar(st.recoilSide, st.recoilSideVel, 0, RC.K, recD, dt)
	st.recoilBack, st.recoilBackVel =
		springScalar(st.recoilBack, st.recoilBackVel, 0, RC.K * 1.4, recD * 1.15, dt)
	st.recoilRise = math.clamp(st.recoilRise, -RC.MAX_RISE, RC.MAX_RISE)
	st.recoilSide = math.clamp(st.recoilSide, -RC.MAX_SIDE, RC.MAX_SIDE)
	st.recoilBack = math.clamp(st.recoilBack, -RC.MAX_BACK, RC.MAX_BACK)
	st.bloom = math.max(0, st.bloom - def.bloomDecay * dt)

	----------------------------------------------------------------------------
	-- Camera motion.
	--
	-- Footfalls are IMPULSES into springs now, not a sine written onto
	-- position. A sine is a shape and reads as a machine; an impulse into a
	-- damped spring is an event and reads as a foot hitting concrete. The
	-- residual sine is still here, an order smaller, riding on top.
	----------------------------------------------------------------------------
	local CS = Cfg.CAMSHAKE
	local camSlack = 1 - a * (1 - CS.ADS_SCALE)

	local vel = root.AssemblyLinearVelocity
	local localVel = bodyCF:VectorToObjectSpace(vel)
	local speedFrac = math.clamp(Vector3.new(vel.X, 0, vel.Z).Magnitude / Cfg.MOVE.WALK, 0, 2.2)

	local prevBob = st.bobPhase
	st.bobPhase += dt * Cfg.CAM.BOB_SPEED * speedFrac
	if speedFrac > 0.15 and math.floor(st.bobPhase / math.pi) > math.floor(prevBob / math.pi) then
		local f = math.min(
			math.min(speedFrac, 1.6) * (st.sprinting and CS.SPRINT_STEP_MULT or 1),
			CS.STEP_FORCE_MAX
		)
		-- Alternating feet, so the roll swaps side each step instead of
		-- pumping the same direction twice a cycle.
		local foot = (math.floor(st.bobPhase / math.pi) % 2 == 0) and 1 or -1
		stepDipVel += CS.STEP_DIP * math.sqrt(CS.STEP_K) * f * camSlack
		stepRollVel += CS.STEP_ROLL * math.sqrt(CS.STEP_K) * f * camSlack * foot
		inertiaPosVel += Vector3.new(0, -Cfg.INERTIA.STEP_IMPULSE * f, 0)
		-- Shoved along the direction of travel, not just downward. Landing on a
		-- foot while moving forward checks your forward motion for a moment.
		inertiaPosVel += Vector3.new(0, 0, -CS.STEP_PUSH * f * math.sign(localVel.Z))

		------------------------------------------------------------------------
		-- FOOTSTEP AUDIO. This did not exist at all: the server was publishing
		-- movement noise for the entity to hear and the player heard silence.
		-- Locomotion weight is carried mostly by ears, and no amount of camera
		-- dip substitutes for it -- silent walking always reads as sliding.
		------------------------------------------------------------------------
		local stepDef = footstepDef()
		if not st.slowWalk and not st.crouched then
			playLocal(stepDef, 0.10)
			if st.sprinting then
				playLocal(Cfg.SFX.GEAR_RATTLE, 0.18)
			end
		else
			-- Quiet movement keeps the surface signature but removes most of the
			-- impact energy instead of switching to an unrelated generic sample.
			local quiet = table.clone(stepDef)
			quiet.vol = (quiet.vol or 0.2) * 0.46
			if quiet.layers then
				quiet.layers = {}
				for _, source in stepDef.layers do
					local layer = table.clone(source)
					layer.vol = (layer.vol or 0.2) * 0.46
					table.insert(quiet.layers, layer)
				end
			end
			playLocal(quiet, 0.13)
		end
	end

	stepDip, stepDipVel = springScalar(stepDip, stepDipVel, 0, CS.STEP_K, CS.STEP_D, dt)
	stepRoll, stepRollVel = springScalar(stepRoll, stepRollVel, 0, CS.STEP_K, CS.STEP_D, dt)
	landDip, landDipVel = springScalar(landDip, landDipVel, 0, CS.LAND_K, CS.LAND_D, dt)

	local bobY = math.abs(math.sin(st.bobPhase)) * CS.BOB_POS * speedFrac * camSlack
	local bobX = math.sin(st.bobPhase * 0.5) * CS.BOB_LATERAL * speedFrac * camSlack
	local bobRoll = math.sin(st.bobPhase * 0.5) * CS.BOB_ROLL * speedFrac * camSlack

	local lateral = math.clamp(localVel.X / Cfg.MOVE.WALK, -1, 1)
	strafeRoll, strafeRollVel = springScalar(
		strafeRoll, strafeRollVel,
		-lateral * CS.STRAFE_ROLL * camSlack,
		CS.STRAFE_K, CS.STRAFE_D, dt
	)

	----------------------------------------------------------------------------
	-- THE KINEMATIC CHAIN.
	--
	--   hips --(waist pivot)--> chest --> neck --> eye
	--                             |
	--                             +--> shoulder --> arms(bore) --> weapon
	--
	-- The weapon hangs off the CHEST. It used to hang off the camera, which is
	-- the single line that made every other feel change pointless: a rigid
	-- child of a transform that snaps cannot lag, whatever spring you put on it.
	----------------------------------------------------------------------------
	local CHEST_BASE = Cfg.MOVE.EYE_HEIGHT_STAND - B.CHEST_TO_NECK - B.NECK_TO_EYE.Y
	local chestDrop = Cfg.MOVE.EYE_HEIGHT_STAND - eyeHeight
	local leanAngle = st.lean * Cfg.MOVE.LEAN_ANGLE

	-- Leaning is a ROTATION ABOUT THE WAIST, written with an explicit pivot,
	-- not a sideways slide plus a roll. The head then travels the arc it
	-- really travels -- out AND down -- and it comes out of the geometry
	-- instead of needing its own hand-tuned offset.
	-- The actual Motor6D body is now the kinematic chain. RootJoint and Waist
	-- carry the lean; Neck carries the final head/free-look rotation. The camera
	-- is then read from the real Head instead of reconstructing a detached eye.
	local theoreticalChestBase = CFrame.new(root.Position)
		* CFrame.Angles(0, st.hipYaw, 0)
		* CFrame.new(0, CHEST_BASE - chestDrop, 0)
		* Weapons.pivoted(B.WAIST_PIVOT, CFrame.Angles(0, 0, -leanAngle))
	local theoreticalChest = theoreticalChestBase
		* CFrame.Angles(0, st.torsoYaw - st.hipYaw, 0)
		* CFrame.Angles(st.torsoPitch, 0, 0)

	if fpBodyRig then
		local humState = hum:GetState()
		local airborne = hum.FloorMaterial == Enum.Material.Air
			or humState == Enum.HumanoidStateType.Jumping
			or humState == Enum.HumanoidStateType.Freefall
		SoldierVisual.poseBody(fpBodyRig, {
			dt = dt,
			lean = st.lean,
			torsoYaw = st.torsoYaw - st.hipYaw,
			torsoPitch = st.torsoPitch,
			headYaw = st.headYaw + st.freeYaw + breathYaw - st.hipYaw,
			headPitch = st.headPitch + st.freePitch + breathPitch,
			headRoll = st.headRoll + strafeRoll + bobRoll + stepRoll,
			stance = st.stance,
			speed = math.clamp(Vector3.new(vel.X, 0, vel.Z).Magnitude / Cfg.MOVE.WALK, 0, 1.35),
			sprint = st.sprinting,
			ads = st.adsAlpha,
			moveX = math.clamp(localVel.X / math.max(Cfg.MOVE.WALK, 0.01), -1, 1),
			moveZ = math.clamp(localVel.Z / math.max(Cfg.MOVE.WALK, 0.01), -1, 1),
			airborne = airborne,
			verticalVelocity = vel.Y,
		})
	end
	chestCF = (fpBodyRig and SoldierVisual.getChestCF(fpBodyRig)) or theoreticalChest

	local headCF = fpBodyRig and SoldierVisual.getHeadCF(fpBodyRig)

	-- TRUE-BODY CAMERA, WITHOUT TRANSFORM FEEDBACK.
	--
	-- Position is anchored to the real Head, so crouch, lean, landing and the
	-- custom body all move the player's eyes. Rotation is NOT read back from
	-- Head.CFrame. The same render step just wrote RootJoint/Waist/Neck.Transform;
	-- reading that hierarchy back immediately made the camera depend on Roblox's
	-- animation evaluation order. That is why vertical look vanished and why the
	-- view shook or snapped on some frames. Mouse look is now authoritative and
	-- the visible neck/torso follow it as a separate, lagged visual chain.
	local viewYaw = st.aimYaw + st.freeYaw + breathYaw
	local viewPitch = math.clamp(
		st.aimPitch + st.freePitch + breathPitch,
		-Cfg.CAM.PITCH_LIMIT, Cfg.CAM.PITCH_LIMIT
	)
	local viewRoll = -leanAngle + st.headRoll + strafeRoll + bobRoll + stepRoll
	local viewRot = CFrame.Angles(0, viewYaw, 0)
		* CFrame.Angles(viewPitch, 0, 0)
		* CFrame.Angles(0, 0, viewRoll)

	local targetEyePos: Vector3
	if headCF then
		local head = character and character:FindFirstChild("Head")
		local headSize = head and head:IsA("BasePart") and head.Size or Vector3.new(1, 1, 1)
		local anatomicalEye = Vector3.new(0, headSize.Y * .10, -headSize.Z * .47)
		targetEyePos = headCF.Position + viewRot:VectorToWorldSpace(anatomicalEye)
	else
		local neckPos = (theoreticalChestBase * CFrame.new(0, B.CHEST_TO_NECK, 0)).Position
		targetEyePos = neckPos + viewRot:VectorToWorldSpace(B.NECK_TO_EYE)
	end
	targetEyePos += viewRot:VectorToWorldSpace(Vector3.new(bobX, bobY - stepDip - landDip, 0))

	-- Remove one-frame Motor6D/physics jitter without adding perceptible camera
	-- latency. Teleports and respawns snap immediately instead of springing from
	-- the old character position.
	if not st.cameraEyePos or (targetEyePos - st.cameraEyePos).Magnitude > 4 then
		st.cameraEyePos = targetEyePos
		st.cameraEyeVel = Vector3.zero
	else
		st.cameraEyePos, st.cameraEyeVel = springStep(
			st.cameraEyePos, st.cameraEyeVel, targetEyePos, 2500, 100, dt
		)
	end

	local cf = CFrame.new(st.cameraEyePos) * viewRot
	if actionPose then cf *= actionPose.camera end
	cam.CFrame = cf
	cam.Focus = cf * CFrame.new(0, 0, -64)
	if fpBodyRig then SoldierVisual.updateBody(fpBodyRig, cf, true) end

	local shoulderCF = chestCF * CFrame.new(0, B.CHEST_TO_SHOULDER, 0)

	-- FREE AIM HAPPENS HERE -- at the shoulder, not at the eye. Swinging the
	-- weapon nine degrees about your shoulder both rotates it AND sweeps it
	-- across the screen, which is what an arm does. Swinging it about your
	-- eyeball only rotates it, which is what a camera filter does, and that is
	-- why more spring and more lag kept making the wrong motion bigger.
	local armCF = shoulderCF
		* CFrame.Angles(0, st.aimYaw - st.torsoYaw, 0)
		* CFrame.Angles(st.aimPitch - st.torsoPitch - st.sag, 0, 0)

	-- FOV rides the same alpha the weapon does, so the zoom and the sights
	-- arrive together instead of racing each other on separate blend rates.
	st.hipFov = lerpNum(st.hipFov, st.sprinting and Cfg.CAM.FOV_SPRINT or Cfg.CAM.FOV_HIP, dt * Cfg.CAM.FOV_BLEND)
	st.fov = st.hipFov + (def.adsFov - st.hipFov) * a
	-- Firing pushes the FOV out like a pressure wave; being suppressed or hit
	-- pulls it IN, so the world closes on you. Opposite signs on purpose.
	local FX = Cfg.SCREENFX
	cam.FieldOfView = st.fov
		+ Fx.fire * Fx.fire * FX.FIRE_FOV
		+ Fx.suppress * Fx.suppress * FX.SUPPRESS_FOV
		+ Fx.hit * Fx.hit * FX.HIT_FOV

	----------------------------------------------------------------------------
	-- WEAPON COLLISION -- continuous, cast down the bore from the shoulder.
	--
	-- This was a boolean: raycast hits something, snap to HIGH_PORT. Walking
	-- into a doorframe hit you like a gear change. Now the closer the muzzle
	-- gets the further the weapon is pushed up and pulled back, so you feel
	-- the wall through the gun well before it has swung anywhere.
	----------------------------------------------------------------------------
	do
		local params = RaycastParams.new()
		params.FilterType = Enum.RaycastFilterType.Exclude
		params.FilterDescendantsInstances = ignoreList()
		local reach = math.min(def.reach or 2.2, Cfg.VIEWMODEL.COLLISION_CHECK)
		local hit = workspace:Raycast(armCF.Position, armCF.LookVector * reach, params)
		local target = hit and math.clamp(1 - hit.Distance / reach, 0, 1) or 0
		-- Going in is faster than coming out: you get shoved, you recover.
		st.collide = lerpNum(st.collide, target, dt * (target > st.collide and 16 or 7))
		st.highPort = st.collide > Cfg.VIEWMODEL.COLLISION_ADS_BLOCK
	end

	----------------------------------------------------------------------------
	-- Viewmodel pose
	----------------------------------------------------------------------------
	local carry: CFrame
	-- Reload/check/inspect timelines own their complete on-screen pose. The old
	-- chain forced LOW_READY underneath every busy action, lowering the weapon by
	-- another ~0.34 studs before the animation even started. That is why the hands
	-- and magazine were only visible when the player looked at the floor.
	local actionOwnsPose = busy and (
		busyLabel == "reload"
		or busyLabel == "shells"
		or busyLabel == "checkmag"
		or busyLabel == "checkchamber"
		or busyLabel == "inspect"
		or busyLabel == "swap"
	)
	if st.sprinting then
		carry = Cfg.VIEWMODEL.SPRINT
	elseif st.readyDown or (handsBusy and not actionOwnsPose) then
		carry = Cfg.VIEWMODEL.LOW_READY
	else
		carry = Cfg.VIEWMODEL.HIP
	end

	-- Hip, low-ready, sprint and wall-compression are not animations pasted onto
	-- the gun; they are a mass moving between support positions. The old
	-- CFrame:Lerp made the pistol, carbine and shotgun share one identical
	-- exponential curve and could never overshoot. This spring is rated from the
	-- weapon's declared kilograms, so the M870 breaks and settles slower while
	-- the P320 snaps into a presentation without becoming instantaneous.
	local carryTarget = carry:Lerp(Cfg.VIEWMODEL.HIGH_PORT, st.collide)
		* CFrame.new(0, 0, st.collide * Cfg.VIEWMODEL.COLLISION_PULL)
	local targetPos, targetRot = splitPose(carryTarget)
	local massRatio = Cfg.VIEWMODEL.CARRY_REFERENCE_MASS / math.max(def.weight or 1, 0.25)
	local massScale = math.clamp(massRatio ^ Cfg.VIEWMODEL.CARRY_MASS_EXP, 0.72, 1.55)
	local carryPosK = Cfg.VIEWMODEL.CARRY_POS_K * massScale
	local carryRotK = Cfg.VIEWMODEL.CARRY_ROT_K * massScale
	local carryPosD = 2 * math.sqrt(carryPosK) * Cfg.VIEWMODEL.CARRY_DAMP
	local carryRotD = 2 * math.sqrt(carryRotK) * Cfg.VIEWMODEL.CARRY_DAMP
	carryPos, carryPosVel = springStep(carryPos, carryPosVel, targetPos, carryPosK, carryPosD, dt)
	carryRot, carryRotVel = springStep(carryRot, carryRotVel, targetRot, carryRotK, carryRotD, dt)
	carryCF = joinPose(carryPos, carryRot)

	local held = carryCF

	-- Presenting the sights is its own spring, rated from the weapon's mass
	-- and slowed by tired arms. The alpha is allowed slightly past 1 so the
	-- gun settles INTO the shoulder rather than arriving and stopping dead;
	-- CFrame:Lerp extrapolates, which is exactly the overshoot we want.
	local adsK, adsD = Weapons.adsSpring(def, st.fatigue)
	st.adsAlpha, adsVel = springScalar(st.adsAlpha, adsVel, st.ads and 1 or 0, adsK, adsD, dt)
	st.adsAlpha = math.clamp(st.adsAlpha, -0.05, Cfg.ADS.MAX_ALPHA)

	-- SOLVED, not posed: put the optical centre of the sight on the camera's
	-- axis at the right eye relief. Correct under any lean, stance or amount
	-- of torso lag, which a fixed CFrame could never be.
	local adsTarget = Weapons.adsSolve(def, cf, armCF, Cfg.VIEWMODEL.ADS_EYE_RELIEF)
	stanceCF = held:Lerp(adsTarget, st.adsAlpha)

	-- Punch the weapon spring when the sights come up or come down. The
	-- transition itself is smooth; this is the bit of it you feel.
	if st.ads ~= prevAds then
		prevAds = st.ads
		local imp = st.ads and Cfg.ADS.RAISE_IMPULSE or -Cfg.ADS.DROP_IMPULSE
		inertiaPosVel += Vector3.new(0, imp * 0.35, imp)
	end

	-- Six-axis gait. A pure XY sine makes the weapon look like a HUD element
	-- sliding on rails. Arms alternately pull the gun sideways, compress it on
	-- foot plant, and advance/retard it through the step; the rotations are
	-- phase-shifted so the muzzle traces a small lopsided loop instead of an
	-- ellipse that repeats mechanically.
	local G = Cfg.GAIT
	local gaitScale = math.min(speedFrac, G.MAX_SCALE)
	if st.sprinting then
		gaitScale *= G.SPRINT_MULT
	elseif st.crouched then
		gaitScale *= G.CROUCH_MULT
	elseif st.slowWalk then
		gaitScale *= G.SLOW_MULT
	end
	gaitScale *= 1 - a * (1 - G.ADS_SCALE)
	local side = math.sin(st.bobPhase)
	local compression = math.abs(math.cos(st.bobPhase))
	local drive = math.sin(st.bobPhase * 2 + 0.35)
	local bob = CFrame.new(
		side * G.POS_X * gaitScale,
		-compression * G.POS_Y * gaitScale,
		drive * G.POS_Z * gaitScale
	) * CFrame.Angles(
		drive * G.PITCH * gaitScale,
		-side * G.YAW * gaitScale,
		-side * G.ROLL * gaitScale
	)

	----------------------------------------------------------------------------
	-- INERTIA -- driven by ACCELERATION.
	--
	-- The old version chased a target proportional to angular RATE and linear
	-- VELOCITY. That produces a constant offset during a constant-rate turn --
	-- the gun just looks mounted crooked -- and produces nothing at all at the
	-- start and the stop of the turn, which are the only two moments carrying
	-- any information about mass. Doubling those coefficients could only ever
	-- make the crooked offset bigger.
	--
	-- The targets are ZERO now. The gun rests where it is held and is
	-- DISTURBED by a pseudo-force -m*a, exactly like a mass sitting in an
	-- accelerating reference frame, which is exactly what your hands are.
	-- The sustained-turn trailing that used to come from the rate term now
	-- comes from the torso joint, where it physically belongs.
	----------------------------------------------------------------------------
	local IN = Cfg.INERTIA
	local slack = 1 - a * (1 - IN.ADS_SCALE)

	local rawAccel = (localVel - prevVelLocal) * invDt
	prevVelLocal = localVel
	bodyAccel = bodyAccel:Lerp(rawAccel, math.clamp(dt * 25, 0, 1))

	local n, h = stepCount(dt)
	local linDrive = Vector3.new(bodyAccel.X, bodyAccel.Y * 0.45, bodyAccel.Z) * (-IN.POS_ACCEL * slack)
	local rotDrive = Vector3.new(
		headPitchAccel,
		headYawAccel,
		-headYawAccel * IN.ROT_ROLL_COUPLE
	) * (-IN.ROT_ACCEL * slack)
	for _ = 1, n do
		inertiaPosVel += linDrive * h
		inertiaRotVel += rotDrive * h
	end

	inertiaRot, inertiaRotVel = springStep(inertiaRot, inertiaRotVel, Vector3.zero, IN.ROT_K, IN.ROT_D, dt)
	inertiaPos, inertiaPosVel = springStep(inertiaPos, inertiaPosVel, Vector3.zero, IN.POS_K, IN.POS_D, dt)

	inertiaRot = Vector3.new(
		math.clamp(inertiaRot.X, -IN.MAX_ROT, IN.MAX_ROT),
		math.clamp(inertiaRot.Y, -IN.MAX_ROT, IN.MAX_ROT),
		math.clamp(inertiaRot.Z, -IN.MAX_ROT, IN.MAX_ROT)
	)
	inertiaPos = Vector3.new(
		math.clamp(inertiaPos.X, -IN.MAX_POS, IN.MAX_POS),
		math.clamp(inertiaPos.Y, -IN.MAX_POS, IN.MAX_POS),
		math.clamp(inertiaPos.Z, -IN.MAX_POS, IN.MAX_POS)
	)

	----------------------------------------------------------------------------
	-- Idle drift -- two detuned sines per axis so it never visibly repeats.
	-- Tired arms wander further, which is the cost the low ready buys off.
	----------------------------------------------------------------------------
	local idleAmp = (st.holdingBreath and IN.IDLE_AMP_ADS * 0.4)
		or (st.ads and IN.IDLE_AMP_ADS)
		or IN.IDLE_AMP
	idleAmp *= exertion
		* (1 + st.fatigue * (Cfg.ARM_STAMINA.SWAY_MULT - 1))
		* armPenalty(Cfg.HEALTH.ARM_SWAY_MAX)
	st.idlePhase += dt * IN.IDLE_SPEED
	local ip = st.idlePhase
	local idleRot = CFrame.Angles(
		(math.sin(ip) + math.sin(ip * 2.31) * 0.45) * idleAmp,
		(math.sin(ip * 0.73 + 1.3) + math.sin(ip * 1.91 + 0.4) * 0.35) * idleAmp * 1.4,
		math.sin(ip * 0.53) * idleAmp * 0.7
	)

	----------------------------------------------------------------------------
	-- EXPLICIT PIVOTS.
	--
	-- Recoil turns about the BUTT PAD in your shoulder, so the muzzle swings
	-- through a long arc and the receiver under your eye barely moves. Sway
	-- and idle turn about the WRIST, because that is the joint the gun
	-- actually hangs from. Both used to turn about the weapon root -- which,
	-- with the viewmodel parented to the camera, meant they turned about the
	-- player's eyeball, and a rifle pivoting about your eye reads as a filter
	-- on the screen rather than as an object with mass in your hands.
	----------------------------------------------------------------------------
	local pocket = def.pocket or Vector3.zero
	local wrist = def.wrist or Vector3.zero

	shotKickPos, shotKickPosVel = springStep(shotKickPos, shotKickPosVel, Vector3.zero, Cfg.RECOIL.SNAP_K, Cfg.RECOIL.SNAP_D, dt)
	shotKickRot, shotKickRotVel = springStep(shotKickRot, shotKickRotVel, Vector3.zero, Cfg.RECOIL.SNAP_K, Cfg.RECOIL.SNAP_D, dt)
	local shotKickCF = Weapons.pivoted(pocket, CFrame.Angles(shotKickRot.X, shotKickRot.Y, shotKickRot.Z)) * CFrame.new(shotKickPos)

	local recoilCF = Weapons.pivoted(
		pocket,
		CFrame.Angles(st.recoilRise, st.recoilSide, st.recoilSide * 0.6)
	) * CFrame.new(0, 0, st.recoilBack)

	local inertiaCF = Weapons.pivoted(
		wrist,
		CFrame.Angles(inertiaRot.X, inertiaRot.Y, inertiaRot.Z)
	) * CFrame.new(inertiaPos)

	local idleCF = Weapons.pivoted(wrist, idleRot)

	-- armCF is the shoulder and the bore; stanceCF is how the weapon is held
	-- off it; everything after that is the weapon's own dynamics.
	vmRootCF = armCF * stanceCF * bob * shotKickCF * recoilCF * inertiaCF * idleCF

	-- Rounds leave along the WEAPON, including its sway and its recoil. Where
	-- the gun is actually pointing is where the bullet goes -- which is the
	-- only honest way to run a game with no crosshair.
	do
		local muzzleLocal = rig and rig.muzzle or Vector3.new(0, 0.3, -2)
		local muzzleWorld = (vmRootCF * CFrame.new(muzzleLocal)).Position
		aimWorldCF = CFrame.lookAt(muzzleWorld, muzzleWorld + vmRootCF.LookVector)
	end

	-- Heat smoke survives the flash and makes rapid strings leave a visible
	-- consequence. It is sparse enough not to become a permanent fog machine.
	local MZ = Cfg.MUZZLE
	st.weaponHeat = math.max(0, st.weaponHeat - MZ.HEAT_COOL * dt)
	if rig and rig.barrelSmoke then
		local over = math.max(0, st.weaponHeat - MZ.HEAT_THRESHOLD)
		rig.barrelSmoke.Rate = over * MZ.HEAT_SMOKE_RATE
	end

	if rig and actionPose then
		actionPose.flashRoll = flashRoll
		local weaponWorldCF, handL, handR = VM.update(rig, vmRootCF, shoulderCF, actionPose)
		if fpBodyRig then
			-- The weapon CFrame lets the body solve the hands against the
			-- weapon's own measured grip frames instead of the viewmodel's
			-- hand targets. See SoldierVisual.poseArms / WeaponGrips.
			SoldierVisual.poseArms(fpBodyRig, chestCF, handL, handR, actionPose,
				weaponWorldCF, rig.def)
			-- Imported custom-mesh limbs follow the now-posed R15 skeleton.
			SoldierVisual.updateBody(fpBodyRig, cam.CFrame, true)
		end
	end

	----------------------------------------------------------------------------
	-- Laser
	----------------------------------------------------------------------------
	if rig then
		local beam, dot = rig.laser, rig.laserDot
		if st.laser and not st.sprinting then
			local from = (vmRootCF * CFrame.new(rig.muzzle)).Position
			local params = RaycastParams.new()
			params.FilterType = Enum.RaycastFilterType.Exclude
			params.FilterDescendantsInstances = ignoreList()
			local aimDir = aimWorldCF.LookVector
			local res = workspace:Raycast(from, aimDir * Cfg.ATMO.LASER_RANGE, params)
			local hitPos = res and res.Position or (from + aimDir * Cfg.ATMO.LASER_RANGE)
			local dist = math.max((hitPos - from).Magnitude, 0.1)

			beam.Size = Vector3.new(0.02, 0.02, dist)
			beam.CFrame = CFrame.lookAt(from, hitPos) * CFrame.new(0, 0, -dist * 0.5)
			beam.Transparency = 0.55
			dot.CFrame = CFrame.new(hitPos)
			dot.Transparency = res and 0 or 1
		else
			beam.Transparency = 1
			dot.Transparency = 1
		end
	end

	----------------------------------------------------------------------------
	-- The local body is updated above from the real Head/Torso/arm skeleton.
	-- CharacterVisuals owns only remote players.
	----------------------------------------------------------------------------

	----------------------------------------------------------------------------
	-- Trigger, HUD, server sync
	----------------------------------------------------------------------------
	if st.triggerDown then
		tryFire()
	end

	if debugFx then
		-- Also reports whether the mesh path or the procedural fallback is live,
		-- and where the flash is being drawn -- the two things that made the
		-- muzzle effects invisible before.
		Hud.objective(("FX  fire %.2f  supp %.2f  hit %.2f  deaf %.2f   |   mesh %s  muzzleZ %.2f  shots %d")
			:format(
				Fx.fire, Fx.suppress, Fx.hit, Fx.deafen,
				rig and rig.visualSource or "none",
				rig and rig.muzzle.Z or 0,
				shotCount
			))
	end

	updateInteract()
	Hud.setBreath(
		st.holdingBreath and (st.breath / Cfg.CAM.BREATH_MAX) or (st.stamina / Cfg.MOVE.STAMINA_MAX),
		st.holdingBreath
	)
	local bloodFraction = math.clamp(traumaNumber("BloodFraction", 1), 0, 1)
	Hud.setHealth(math.min(hum.Health / hum.MaxHealth, bloodFraction))
	Hud.setLimbStatus({
		Head = limbRatio("Head"),
		Torso = limbRatio("Torso"),
		LeftArm = limbRatio("LeftArm"),
		RightArm = limbRatio("RightArm"),
		LeftLeg = limbRatio("LeftLeg"),
		RightLeg = limbRatio("RightLeg"),
	}, player:GetAttribute("Bleeding") == true)
	Hud.setAnatomyStatus({
		bloodMl = traumaNumber("BloodMl", 5000),
		pain = traumaNumber("Pain", 0),
		shock = traumaNumber("Shock", 0),
		externalBleed = traumaNumber("ExternalBleedRate", 0),
		internalBleed = traumaNumber("InternalBleedRate", 0),
		lastBone = player:GetAttribute("WorstFractureBone") or player:GetAttribute("LastBrokenBone") or "",
		lastOrgan = player:GetAttribute("LastDamagedOrgan") or "",
		fracture = traumaNumber("WorstFractureLevel", worstFracture()),
	})

	syncAccum += dt
	if syncAccum >= 0.05 then
		syncAccum = 0
		local current = W()
		rSync:FireServer(
			st.headPitch + st.freePitch,
			stanceName(moving),
			current and current.id or "MK18",
			current and current.suppressed == true or false,
			st.ads,
			st.lean,
			anim and anim.name or "",
			anim and anim.time or 0,
			anim and anim.scale or 1,
			anim and anim.motionSerial or 0
		)
	end
end

RunService:BindToRenderStep("Blacksite", Enum.RenderPriority.Camera.Value, function(dt)
	local ok, err = pcall(renderStep, dt)
	if not ok then
		reportRenderError(err)
	end
end)
