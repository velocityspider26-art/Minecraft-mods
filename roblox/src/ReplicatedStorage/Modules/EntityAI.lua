--!nonstrict
-- EntityAI -- SUBJECT 09.
--
-- Design rule: it is never scary because it is unfair, it is scary because it
-- is *legible*. Every one of its states has a tell you can hear, it only knows
-- what you have actually let it know, and it always gives you a beat to react.
-- It cannot be killed. It can be hurt enough that it leaves, and it comes back.
--
-- Perception is the whole design:
--   * It hears. Sprinting, firing unsuppressed, slamming doors and pulling a
--     power cell all publish a noise with a radius (GameConfig.NOISE).
--   * It sees, but badly in the dark -- unless your weapon light is on, in
--     which case you are the brightest thing in the building.
--   * It remembers for a few seconds, then searches, then loses you.
--
-- Server-side only. Required by GameServer.

local PathfindingService = game:GetService("PathfindingService")
local Players = game:GetService("Players")
local RunService = game:GetService("RunService")

local Cfg = require(script.Parent:WaitForChild("GameConfig"))
local Audio = require(script.Parent:WaitForChild("TacticalAudio"))
local E = Cfg.ENTITY

local EntityAI = {}

type Hooks = {
	damagePlayer: (Model, number) -> (),
	onStateChange: ((string) -> ())?,
}

local model: Model? = nil
local root: BasePart? = nil
local humanoid: Humanoid? = nil
local hooks: Hooks
local map: any = nil

local state = "IDLE"
local stateEnter = 0

local target: Model? = nil -- the character it is currently hunting
local lastSeen = 0
local lastKnownPos: Vector3? = nil

-- How long a noise stays interesting is `stateEnter` plus SEARCH_TIME, not a
-- separate timestamp: once it commits to investigating, the clock that matters
-- is how long it has been searching, not how old the sound was.
local noisePos: Vector3? = nil

local waypoints: { PathWaypoint } = {}
local waypointIndex = 1
local lastPath = 0
local pathGoal: Vector3? = nil

local poise = 0
local poiseTime = 0
local nextAttack = 0
local fleeUntil = 0
local powered = false

local rng = Random.new()

--------------------------------------------------------------------------------
-- Rig
--------------------------------------------------------------------------------

local function limb(parent: Model, name: string, size: Vector3, cf: CFrame, color: Color3, mat: Enum.Material): BasePart
	local p = Instance.new("Part")
	p.Name = name
	p.Size = size
	p.CFrame = cf
	p.Color = color
	p.Material = mat
	p.Anchored = false
	p.CanCollide = false
	p.Massless = true
	p.TopSurface = Enum.SurfaceType.Smooth
	p.BottomSurface = Enum.SurfaceType.Smooth
	p.Parent = parent
	return p
end

local function sound(parent: Instance, def: { id: string, vol: number, pitch: number }, looped: boolean): Sound
	local s = Instance.new("Sound")
	s.SoundId = def.id
	s.Volume = def.vol
	s.PlaybackSpeed = def.pitch
	s.Looped = looped
	s.RollOffMode = Enum.RollOffMode.InverseTapered
	s.RollOffMinDistance = 8
	s.RollOffMaxDistance = 220
	s.Parent = parent
	return s
end

local breathSound: Sound

local function playEntity(def: any, jitter: number?)
	if root and root.Parent then
		Audio.playWorld(def, root.Position, jitter or 0.04, model and { model } or nil)
	end
end

local function buildRig(spawnPos: Vector3): Model
	local m = Instance.new("Model")
	m.Name = E.NAME

	local skin = Color3.fromRGB(24, 23, 25)

	local hrp = Instance.new("Part")
	hrp.Name = "HumanoidRootPart"
	hrp.Size = Vector3.new(2.4, 2.2, 1.4)
	hrp.CFrame = CFrame.new(spawnPos + Vector3.new(0, E.HIP_HEIGHT + 1.1, 0))
	hrp.Transparency = 1
	hrp.CanCollide = true
	hrp.Anchored = false
	hrp.Parent = m
	m.PrimaryPart = hrp

	local base = hrp.CFrame
	local function at(dx: number, dy: number, dz: number): CFrame
		return base * CFrame.new(dx, dy, dz)
	end

	limb(m, "Torso", Vector3.new(2.5, 3.0, 1.25), at(0, 0.3, 0), skin, Enum.Material.Sand)
	local head = limb(m, "Head", Vector3.new(1.25, 1.45, 1.5), at(0, 2.2, -0.15), skin, Enum.Material.Sand)
	limb(m, "LeftArm", Vector3.new(0.62, 4.3, 0.62), at(-1.6, 0.2, 0), skin, Enum.Material.Sand)
	limb(m, "RightArm", Vector3.new(0.62, 4.3, 0.62), at(1.6, 0.2, 0), skin, Enum.Material.Sand)
	limb(m, "LeftLeg", Vector3.new(0.78, 3.6, 0.78), at(-0.62, -2.9, 0), skin, Enum.Material.Sand)
	limb(m, "RightLeg", Vector3.new(0.78, 3.6, 0.78), at(0.62, -2.9, 0), skin, Enum.Material.Sand)

	-- Two dim eyes. This is the single most important readability decision in
	-- the game: in a pitch-black corridor they are the only warning you get,
	-- and they are dim enough that you have to be looking the right way.
	for _, side in { -0.3, 0.3 } do
		local eye = limb(
			m,
			"Eye",
			Vector3.new(0.16, 0.09, 0.1),
			head.CFrame * CFrame.new(side, 0.18, -0.75),
			Color3.fromRGB(255, 236, 214),
			Enum.Material.Neon
		)
		local pl = Instance.new("PointLight")
		pl.Color = Color3.fromRGB(255, 210, 170)
		pl.Brightness = 0.55
		pl.Range = 7
		pl.Parent = eye
	end

	for _, p in m:GetChildren() do
		if p:IsA("BasePart") and p ~= hrp then
			local wc = Instance.new("WeldConstraint")
			wc.Part0 = hrp
			wc.Part1 = p
			wc.Parent = hrp
		end
	end

	local hum = Instance.new("Humanoid")
	-- R15 is what makes HipHeight mean "distance from the bottom of the root
	-- part to the floor", which is the number the rig above is laid out around.
	-- Leave it on the R6 default and the whole thing sinks into the concrete.
	hum.RigType = Enum.HumanoidRigType.R15
	hum.MaxHealth = 1e6
	hum.Health = 1e6
	hum.WalkSpeed = E.SPEED_IDLE
	hum.HipHeight = E.HIP_HEIGHT
	hum.AutoRotate = true
	hum.BreakJointsOnDeath = false
	hum.DisplayDistanceType = Enum.HumanoidDisplayDistanceType.None
	hum.HealthDisplayType = Enum.HumanoidHealthDisplayType.AlwaysOff
	hum.Parent = m

	breathSound = sound(hrp, Cfg.SFX.ENTITY_BREATH, true)
	-- Keep only the continuous breath as a loop. Footfalls and screams go
	-- through TacticalAudio so enclosure, distance and reflections match the
	-- rest of the game instead of sounding like flat sounds welded to the NPC.
	local breathEq = Instance.new("EqualizerSoundEffect")
	breathEq.LowGain = 3
	breathEq.MidGain = -2
	breathEq.HighGain = -9
	breathEq.Parent = breathSound
	local breathComp = Instance.new("CompressorSoundEffect")
	breathComp.Attack = 0.04
	breathComp.Release = 0.32
	breathComp.Ratio = 3
	breathComp.Threshold = -18
	breathComp.Parent = breathSound
	breathSound:Play()

	m.Parent = workspace
	return m
end

--------------------------------------------------------------------------------
-- Perception helpers
--------------------------------------------------------------------------------

local function alivePlayers(): { Model }
	local out = {}
	for _, plr in Players:GetPlayers() do
		local char = plr.Character
		local hum = char and char:FindFirstChildOfClass("Humanoid")
		if char and hum and hum.Health > 0 and char.PrimaryPart then
			table.insert(out, char)
		end
	end
	return out
end

local function hasLineOfSight(from: Vector3, char: Model): boolean
	local hrp = char.PrimaryPart
	if not hrp then
		return false
	end
	local params = RaycastParams.new()
	params.FilterType = Enum.RaycastFilterType.Exclude
	params.FilterDescendantsInstances = { model :: Instance }
	params.IgnoreWater = true

	local to = hrp.Position + Vector3.new(0, 1.2, 0)
	local res = workspace:Raycast(from, to - from, params)
	if not res then
		return true
	end
	return res.Instance:IsDescendantOf(char)
end

-- How far it can see this particular player right now.
local function sightRange(char: Model): number
	if char:GetAttribute("TorchOn") then
		return E.SIGHT_RANGE_LIGHT_ON
	end
	if char:GetAttribute("LaserOn") then
		return E.SIGHT_RANGE_DARK * 2.4
	end
	return powered and E.SIGHT_RANGE_LIT or E.SIGHT_RANGE_DARK
end

local function facingCone(fromCF: CFrame, point: Vector3): boolean
	local dir = point - fromCF.Position
	if dir.Magnitude < 0.1 then
		return true
	end
	local angle = math.acos(math.clamp(fromCF.LookVector:Dot(dir.Unit), -1, 1))
	return angle <= E.SIGHT_FOV * 0.5
end

--------------------------------------------------------------------------------
-- State machine
--------------------------------------------------------------------------------

local function setState(new: string)
	if state == new then
		return
	end
	state = new
	stateEnter = os.clock()
	waypoints = {}
	waypointIndex = 1
	pathGoal = nil
	if hooks.onStateChange then
		hooks.onStateChange(new)
	end
end

local function speedFor(s: string): number
	local mult = powered and E.POWERED_AGGRESSION or 1
	if s == "HUNT" then
		return E.SPEED_HUNT * mult
	elseif s == "ATTACK" then
		return E.SPEED_CHARGE * mult
	elseif s == "INVESTIGATE" then
		return E.SPEED_INVESTIGATE * mult
	elseif s == "FLEE" then
		return E.FLEE_SPEED
	end
	return E.SPEED_IDLE
end

-- Doors do not stop it. They cost it two seconds and tell you exactly where it
-- is, which is the trade the player is actually buying when they close one.
local function forceNearbyDoors()
	if not map or not root then
		return
	end
	for _, door in map.doors do
		if door:GetAttribute("Closed") then
			local leaf = door.PrimaryPart
			if leaf and (leaf.Position - root.Position).Magnitude < 9 then
				EntityAI.slamDoor(door)
			end
		end
	end
end

local doorSlamCallback: ((Model) -> ())? = nil
function EntityAI.setDoorSlamCallback(fn: (Model) -> ())
	doorSlamCallback = fn
end
function EntityAI.slamDoor(door: Model)
	if doorSlamCallback then
		doorSlamCallback(door)
	end
end

local function repath(goal: Vector3)
	if not root then
		return
	end
	local now = os.clock()
	local goalMoved = pathGoal == nil or (goal - pathGoal).Magnitude > 8
	if not goalMoved and now - lastPath < E.REPATH then
		return
	end
	lastPath = now
	pathGoal = goal

	local path = PathfindingService:CreatePath({
		AgentRadius = 2.6,
		AgentHeight = 7.5,
		AgentCanJump = false,
		WaypointSpacing = 6,
	})

	local ok = pcall(function()
		path:ComputeAsync(root.Position, goal)
	end)

	if ok and path.Status == Enum.PathStatus.Success then
		waypoints = path:GetWaypoints()
		waypointIndex = math.min(2, #waypoints) -- skip the node under its feet
	else
		-- Pathfinding failed (usually a goal inside geometry). Walk the straight
		-- line instead; it will bump a wall, but it will not freeze.
		waypoints = {}
		waypointIndex = 1
	end
end

local function follow(goal: Vector3)
	if not humanoid or not root then
		return
	end
	repath(goal)

	if #waypoints > 0 and waypointIndex <= #waypoints then
		local wp = waypoints[waypointIndex]
		if (Vector3.new(wp.Position.X, 0, wp.Position.Z) - Vector3.new(root.Position.X, 0, root.Position.Z)).Magnitude < 4 then
			waypointIndex += 1
		end
		if waypointIndex <= #waypoints then
			humanoid:MoveTo(waypoints[waypointIndex].Position)
			return
		end
	end
	humanoid:MoveTo(goal)
end

local function pickPatrolNode(awayFrom: Vector3?): Vector3
	if not map or #map.patrol == 0 then
		return Vector3.new(0, 3, 0)
	end
	if awayFrom then
		local best, bestD = map.patrol[1], -1
		for _ = 1, 8 do
			local cand = map.patrol[rng:NextInteger(1, #map.patrol)]
			local d = (cand - awayFrom).Magnitude
			if d > bestD then
				best, bestD = cand, d
			end
		end
		return best
	end
	return map.patrol[rng:NextInteger(1, #map.patrol)]
end

--------------------------------------------------------------------------------
-- Public API
--------------------------------------------------------------------------------

-- Publish a noise. The entity only reacts if it is inside the radius, so the
-- caller expresses loudness in studs and this stays honest.
function EntityAI.hear(position: Vector3, radius: number)
	if not root or state == "FLEE" then
		return
	end
	local d = (position - root.Position).Magnitude
	if d > radius * E.HEAR_MULT * (powered and E.POWERED_AGGRESSION or 1) then
		return
	end
	-- A louder-than-necessary noise while already hunting does not distract it.
	if state == "HUNT" or state == "ATTACK" then
		return
	end
	noisePos = position
	if state ~= "INVESTIGATE" then
		setState("INVESTIGATE")
	else
		stateEnter = os.clock()
	end
end

function EntityAI.damage(amount: number, from: Vector3?)
	if not root or state == "FLEE" then
		return
	end
	local now = os.clock()
	if now - poiseTime > E.POISE_WINDOW then
		poise = 0
	end
	poiseTime = now
	poise += amount

	if from then
		noisePos = from
	end

	if poise >= E.POISE then
		poise = 0
		fleeUntil = now + E.FLEE_TIME
		playEntity(Cfg.SFX.ENTITY_SCREAM, 0.035)
		setState("FLEE")
	elseif state ~= "HUNT" and state ~= "ATTACK" then
		-- Being shot at from the dark is enough to bring it straight at you.
		if from then
			lastKnownPos = from
			lastSeen = now
		end
		setState("HUNT")
	end
end

function EntityAI.setPowered(v: boolean)
	powered = v
end

function EntityAI.getModel(): Model?
	return model
end

function EntityAI.getState(): string
	return state
end

--------------------------------------------------------------------------------
-- Tick
--------------------------------------------------------------------------------

local lastStep = 0

local function tick(dt: number)
	if not model or not model.Parent or not root or not humanoid then
		return
	end
	local now = os.clock()
	humanoid.WalkSpeed = speedFor(state)

	-- Footfalls, so you can hear it coming through a wall.
	if humanoid.MoveDirection.Magnitude > 0.1 then
		local interval = math.clamp(9 / math.max(humanoid.WalkSpeed, 1), 0.28, 1.4)
		if now - lastStep > interval then
			lastStep = now
			playEntity(Cfg.SFX.ENTITY_STEP, 0.08)
		end
	end

	breathSound.Volume = Cfg.SFX.ENTITY_BREATH.vol * (state == "HUNT" and 1.8 or 1.0)
	forceNearbyDoors()

	--------------------------------------------------------------------------
	-- Sight check (runs in every state except FLEE)
	--------------------------------------------------------------------------
	if state ~= "FLEE" then
		local eye = root.CFrame * CFrame.new(0, 2.2, 0)
		local best, bestD = nil, math.huge
		for _, char in alivePlayers() do
			local hrp = char.PrimaryPart :: BasePart
			local d = (hrp.Position - root.Position).Magnitude
			if d < bestD and d <= sightRange(char) then
				if facingCone(root.CFrame, hrp.Position) or d < 12 then
					if hasLineOfSight(eye.Position, char) then
						best, bestD = char, d
					end
				end
			end
		end
		if best then
			target = best
			lastSeen = now
			lastKnownPos = (best.PrimaryPart :: BasePart).Position
			if state ~= "HUNT" and state ~= "ATTACK" then
				playEntity(Cfg.SFX.ENTITY_SCREAM, 0.035)
				setState("HUNT")
			end
		end
	end

	--------------------------------------------------------------------------
	if state == "IDLE" then
		if not pathGoal or (root.Position - pathGoal).Magnitude < 8 then
			pathGoal = nil
			follow(pickPatrolNode())
		else
			follow(pathGoal)
		end
	elseif state == "INVESTIGATE" then
		local goal = noisePos
		if not goal then
			setState("IDLE")
			return
		end
		if (root.Position - goal).Magnitude < 7 then
			-- Arrived. Poke around nearby for a while before giving up.
			if now - stateEnter > E.SEARCH_TIME then
				setState("IDLE")
			elseif not pathGoal or (root.Position - pathGoal).Magnitude < 6 then
				local jitter = Vector3.new(rng:NextNumber(-30, 30), 0, rng:NextNumber(-30, 30))
				follow(goal + jitter)
			end
		else
			follow(goal)
			if now - stateEnter > E.SEARCH_TIME * 2 then
				setState("IDLE")
			end
		end
	elseif state == "HUNT" or state == "ATTACK" then
		local char = target
		local hum = char and char:FindFirstChildOfClass("Humanoid")
		if not char or not char.Parent or not hum or hum.Health <= 0 then
			target = nil
			setState("IDLE")
			return
		end

		local hrp = char.PrimaryPart
		if not hrp then
			setState("IDLE")
			return
		end

		local d = (hrp.Position - root.Position).Magnitude
		local fresh = now - lastSeen < E.HUNT_MEMORY

		if d <= E.ATTACK_RANGE and hasLineOfSight((root.CFrame * CFrame.new(0, 2.2, 0)).Position, char) then
			setState("ATTACK")
			humanoid:MoveTo(hrp.Position)
			if now >= nextAttack then
				nextAttack = now + E.ATTACK_COOLDOWN
				playEntity(Cfg.SFX.ENTITY_SCREAM, 0.035)
				local victim = char
				task.delay(E.ATTACK_WINDUP, function()
					if not root or not victim.Parent then
						return
					end
					local vhrp = victim.PrimaryPart
					if vhrp and (vhrp.Position - root.Position).Magnitude <= E.ATTACK_RANGE * 1.4 then
						hooks.damagePlayer(victim, E.ATTACK_DAMAGE)
					end
				end)
			end
		elseif fresh then
			setState("HUNT")
			lastKnownPos = hrp.Position
			follow(hrp.Position)
		else
			-- Lost you. Go to where you were and start searching.
			noisePos = lastKnownPos
			target = nil
			setState("INVESTIGATE")
		end
	elseif state == "FLEE" then
		if now >= fleeUntil then
			setState("IDLE")
		else
			if not pathGoal or (root.Position - pathGoal).Magnitude < 10 then
				local players = alivePlayers()
				local away = #players > 0 and (players[1].PrimaryPart :: BasePart).Position or nil
				follow(pickPatrolNode(away))
			else
				follow(pathGoal)
			end
		end
	end
end

function EntityAI.start(mapData: any, h: Hooks)
	map = mapData
	hooks = h

	local existing = workspace:FindFirstChild(E.NAME)
	if existing then
		existing:Destroy()
	end

	-- Spawn as far from the insertion point as the patrol graph allows.
	local spawnAt = pickPatrolNode(mapData.spawnCFrame.Position)
	model = buildRig(spawnAt)
	root = (model :: Model):FindFirstChild("HumanoidRootPart") :: BasePart
	humanoid = (model :: Model):FindFirstChildOfClass("Humanoid")

	setState("IDLE")

	-- The brain runs at 12 Hz, not per frame. Humanoid:MoveTo keeps walking
	-- toward its last goal between calls, so the movement stays smooth while
	-- the raycasts, player scan and door sweep happen a fifth as often.
	local TICK = 1 / 12
	local acc = 0
	RunService.Heartbeat:Connect(function(dt)
		acc += dt
		if acc < TICK then
			return
		end
		local step = acc
		acc = 0
		local ok, err = pcall(tick, step)
		if not ok then
			warn("[SUBJECT_09] tick error: " .. tostring(err))
		end
	end)
end

return EntityAI
