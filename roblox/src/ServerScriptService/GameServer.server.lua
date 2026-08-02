--!nonstrict
-- GameServer -- the only Script on the server. Owns everything authoritative:
-- map creation, the entity, damage, the objective chain and noise publishing.
--
-- AUTHORITY MODEL: this is a co-op PvE game, so the client owns *weapon feel*
-- -- ammunition state, reload timing, recoil, sway. Nothing is gained by
-- fighting the client over that, and a lot of responsiveness is lost. The
-- server owns everything that can hurt someone: it re-raycasts every shot from
-- the claimed muzzle, rate-limits fire, and applies all damage itself. A
-- cheater can give themselves infinite ammo in a game with no opponents; they
-- cannot shoot through walls or hit someone they were not pointing at.

local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local Lighting = game:GetService("Lighting")
local StarterPlayer = game:GetService("StarterPlayer")

-- Characters are loaded manually later in this script. Avatar appearance is
-- disabled because BLACKSITE supplies one consistent human tactical body.
pcall(function()
	StarterPlayer.LoadCharacterAppearance = false
end)
pcall(function()
	StarterPlayer.CharacterRigType = Enum.HumanoidRigType.R15
end)
pcall(function()
	-- Higher-fidelity avatar joints allow the same R15 humanoid to use the
	-- optional shoulder, spine and hand constraints when the selected body
	-- bundle supplies them. The call is guarded for Studio channels where the
	-- rollout property is unavailable.
	StarterPlayer.AvatarJointUpgrade = Enum.RolloutState.Enabled
end)

local HumanBody
local Anatomy

local function loadTacticalCharacter(plr: Player)
	local description, bodySource = HumanBody.createDescription()
	plr:SetAttribute("BlacksiteBodySource", bodySource)
	local ok, err = pcall(function()
		plr:LoadCharacterWithHumanoidDescriptionAsync(description)
	end)
	description:Destroy()
	if not ok and plr.Parent then
		warn("[BLACKSITE] hidden R15 skeleton load failed; retrying the stock skeleton:", err)
		plr:LoadCharacter()
	end
end

local Modules = ReplicatedStorage:WaitForChild("Modules")
local Cfg = require(Modules:WaitForChild("GameConfig"))
local Weapons = require(Modules:WaitForChild("Weapons"))
local Ballistics = require(Modules:WaitForChild("Ballistics"))
local MapBuilder = require(Modules:WaitForChild("MapBuilder"))
local ArenaBuilder = require(Modules:WaitForChild("ArenaBuilder"))
local EntityAI = require(Modules:WaitForChild("EntityAI"))
local Audio = require(Modules:WaitForChild("TacticalAudio"))
HumanBody = require(Modules:WaitForChild("HumanBody"))
Anatomy = require(Modules:WaitForChild("Anatomy"))

--------------------------------------------------------------------------------
-- Network surface
--------------------------------------------------------------------------------
local net = Instance.new("Folder")
net.Name = "Net"
net.Parent = ReplicatedStorage

local function remote(name: string, unreliable: boolean?): any
	local r = Instance.new(unreliable and "UnreliableRemoteEvent" or "RemoteEvent")
	r.Name = name
	r.Parent = net
	return r
end

local rFire = remote("Fire")
local rSync = remote("Sync", true) -- look pitch + stance, 20 Hz
local rGear = remote("Gear") -- torch / laser / nvg toggles
local rInteract = remote("Interact")
local rBandage = remote("Bandage")
local rNoise = remote("Noise") -- client-side events worth hearing
local rEffect = remote("Effect") -- server -> client one-shots
local rMessage = remote("Message") -- server -> client subtitle lines

local gameState = Instance.new("Folder")
gameState.Name = "GameState"
gameState.Parent = ReplicatedStorage

--------------------------------------------------------------------------------
-- World
--------------------------------------------------------------------------------
local ARENA = Cfg.MODE == "ARENA"

if ARENA then
	----------------------------------------------------------------------------
	-- Daylight. None of the dark-facility lighting survives contact with the
	-- sun, so this is a separate set rather than a scale on the other one --
	-- notably Brightness, which is the sun and moon and therefore the ONLY
	-- place it has ever done anything, and EnvironmentDiffuseScale, which is
	-- sky-derived fill and is correct outdoors and wrong indoors.
	----------------------------------------------------------------------------
	local D = Cfg.ATMO.DAY
	Lighting.Ambient = D.AMBIENT
	Lighting.OutdoorAmbient = D.OUTDOOR_AMBIENT
	Lighting.Brightness = D.BRIGHTNESS
	Lighting.ExposureCompensation = D.EXPOSURE
	Lighting.ClockTime = D.CLOCK
	Lighting.GeographicLatitude = D.GEO_LATITUDE
	Lighting.GlobalShadows = true
	Lighting.FogColor = D.FOG_COLOR
	Lighting.FogStart = D.FOG_START
	Lighting.FogEnd = D.FOG_END
	Lighting.EnvironmentDiffuseScale = D.ENV_DIFFUSE
	Lighting.EnvironmentSpecularScale = D.ENV_SPECULAR
else
	Lighting.Ambient = Cfg.ATMO.AMBIENT_DARK
	-- NOT black. Effective OutdoorAmbient is max(OutdoorAmbient, Ambient) per
	-- channel, so setting this to zero darkened nothing at all -- it only meant
	-- Ambient's hue leaked into anything that could see sky.
	Lighting.OutdoorAmbient = Cfg.ATMO.OUTDOOR_AMBIENT
	-- Sun and moon only. This has never affected a PointLight or a SpotLight, so
	-- it was not what made the facility dark and raising it would not have helped.
	Lighting.Brightness = Cfg.ATMO.BRIGHTNESS
	-- The master gain, and it was sitting at 0. A pre-tonemap exposure bias in
	-- stops: +1 is twice the light reaching the screen. One stop here is worth
	-- more than any change to an individual fixture, because it multiplies the
	-- light that is already there instead of adding light that is not.
	Lighting.ExposureCompensation = Cfg.ATMO.EXPOSURE
	Lighting.ClockTime = 0
	Lighting.GlobalShadows = true
	Lighting.FogColor = Cfg.ATMO.FOG_COLOR
	Lighting.FogStart = 0
	Lighting.FogEnd = Cfg.ATMO.FOG_END
	-- Correct to keep at 0 indoors: this is sky-derived fill and there is no sky.
	Lighting.EnvironmentDiffuseScale = 0
	Lighting.EnvironmentSpecularScale = 0
end

local map = ARENA and ArenaBuilder.build() or MapBuilder.build()

local cellsTotal = #map.fuses
local cellsInserted = 0
local powered = false

gameState:SetAttribute("CellsTotal", cellsTotal)
gameState:SetAttribute("CellsInserted", 0)
gameState:SetAttribute("Powered", false)
gameState:SetAttribute("Extracted", 0)

--------------------------------------------------------------------------------
-- Sound helper (server-spawned, positional, self-cleaning)
--------------------------------------------------------------------------------
-- TacticalAudio is shared with the client so gunfire, impacts, doors and
-- objectives use one acoustic model instead of two unrelated mixers. On the
-- server it measures enclosure and creates positional layers; each listener's
-- Roblox rolloff then handles distance naturally.
local function playAt(def: any, position: Vector3, maxDist: number?)
	if not def then
		return
	end
	local soundDef = def
	if maxDist then
		-- Do not mutate GameConfig. Clone the top level and any layers so a one-off
		-- distance override cannot leak into later events.
		soundDef = table.clone(def)
		if def.layers then
			soundDef.layers = {}
			for _, source in def.layers do
				local layer = table.clone(source)
				layer.maxDistance = math.min(layer.maxDistance or maxDist, maxDist)
				table.insert(soundDef.layers, layer)
			end
		else
			soundDef.maxDistance = maxDist
		end
	end
	Audio.playWorld(soundDef, position, 0.025, nil)
end

--------------------------------------------------------------------------------
-- Doors
--------------------------------------------------------------------------------
local function setDoor(door: Model, closed: boolean, slam: boolean?)
	local leaf = door.PrimaryPart
	if not leaf then
		return
	end
	door:SetAttribute("Closed", closed)
	leaf.CanCollide = closed
	leaf.CanQuery = closed -- an open doorway must not stop bullets
	leaf.Transparency = closed and 0 or 1

	local source = Cfg.SFX.DOOR
	local def = table.clone(source)
	def.vol = slam and 1.0 or source.vol
	def.pitch = slam and 0.35 or source.pitch
	if source.layers then
		def.layers = {}
		for _, sourceLayer in source.layers do
			local layer = table.clone(sourceLayer)
			if slam then
				layer.vol = (layer.vol or 1) * 1.35
				layer.pitch = (layer.pitch or 1) * 0.78
			end
			table.insert(def.layers, layer)
		end
	end
	playAt(def, leaf.Position)
	EntityAI.hear(leaf.Position, slam and Cfg.NOISE.DOOR * 1.6 or Cfg.NOISE.DOOR)
end

EntityAI.setDoorSlamCallback(function(door)
	setDoor(door, false, true)
end)

--------------------------------------------------------------------------------
-- Players
--------------------------------------------------------------------------------
type PlayerState = {
	pitch: number,
	stance: string,
	bleeding: boolean,
	bandages: number,
	cells: number,
	shots: { number },
	torch: BasePart?,
	weld: Weld?,
	extracted: boolean,
	limbs: { [string]: number },
	limbBand: { [string]: number },
	anatomy: any,
}

local pstate: { [Player]: PlayerState } = {}

local function freshLimbs(): { [string]: number }
	local out = {}
	for group, maximum in Cfg.HEALTH.LIMB_MAX do
		out[group] = maximum
	end
	return out
end

local function state(plr: Player): PlayerState
	local s = pstate[plr]
	if not s then
		s = {
			pitch = 0,
			stance = "still",
			bleeding = false,
			bandages = Cfg.HEALTH.BANDAGE_START_COUNT,
			cells = 0,
			shots = {},
			torch = nil,
			weld = nil,
			extracted = false,
			limbs = freshLimbs(),
			limbBand = {},
			anatomy = Anatomy.newState(),
		}
		pstate[plr] = s
	end
	return s
end

local function setBothAttribute(plr: Player, char: Model?, name: string, value: any)
	plr:SetAttribute(name, value)
	if char then char:SetAttribute(name, value) end
end

local function syncAnatomy(plr: Player, char: Model?, s: PlayerState)
	local anatomy = s.anatomy
	if not anatomy then return end
	local total, external, internal = Anatomy.totalBleed(anatomy)
	local penalties = Anatomy.functionalPenalties(anatomy)
	s.bleeding = total > 0.15

	setBothAttribute(plr, char, "BloodMl", math.floor(anatomy.blood + 0.5))
	setBothAttribute(plr, char, "BloodFraction", math.clamp(anatomy.blood / Anatomy.BLOOD_MAX, 0, 1))
	setBothAttribute(plr, char, "Pain", anatomy.pain)
	setBothAttribute(plr, char, "Shock", anatomy.shock)
	setBothAttribute(plr, char, "BleedRate", total)
	setBothAttribute(plr, char, "ExternalBleedRate", external)
	setBothAttribute(plr, char, "InternalBleedRate", internal)
	setBothAttribute(plr, char, "Bleeding", s.bleeding)
	setBothAttribute(plr, char, "LastBrokenBone", anatomy.lastBone or "")
	setBothAttribute(plr, char, "LastDamagedOrgan", anatomy.lastOrgan or "")
	setBothAttribute(plr, char, "Fracture_LeftLeg", penalties.leftLeg)
	setBothAttribute(plr, char, "Fracture_RightLeg", penalties.rightLeg)
	setBothAttribute(plr, char, "Fracture_LeftArm", penalties.leftArm)
	setBothAttribute(plr, char, "Fracture_RightArm", penalties.rightArm)
	setBothAttribute(plr, char, "Fracture_Axial", penalties.axial)

	local worstLevel, worstLabel = 0, ""
	for _, key in Anatomy.BONE_KEYS do
		local def = Anatomy.BONES[key]
		local value = anatomy.bones[key] or def.max
		local level = Anatomy.fractureLevel(value, def.max)
		setBothAttribute(plr, char, "Bone_" .. key, level)
		if level > worstLevel then
			worstLevel, worstLabel = level, def.label
		end
	end
	setBothAttribute(plr, char, "WorstFractureLevel", worstLevel)
	setBothAttribute(plr, char, "WorstFractureBone", worstLabel)
	for key, def in Anatomy.ORGANS do
		setBothAttribute(plr, char, "Organ_" .. key, math.clamp((anatomy.organs[key] or def.max) / def.max, 0, 1))
	end
end

local function buildTorch(char: Model, s: PlayerState)
	local head = char:FindFirstChild("Head") :: BasePart?
	if not head then
		return
	end

	local p = Instance.new("Part")
	p.Name = "TacLight"
	p.Size = Vector3.new(0.3, 0.3, 0.5)
	p.Transparency = 1
	p.CanCollide = false
	p.CanQuery = false
	p.CanTouch = false
	p.Massless = true
	p.CFrame = head.CFrame
	p.Parent = char

	local weld = Instance.new("Weld")
	weld.Part0 = head
	weld.Part1 = p
	weld.C0 = CFrame.new(0, -0.1, -0.4)
	weld.Parent = p

	local light = Instance.new("SpotLight")
	light.Name = "Beam"
	light.Face = Enum.NormalId.Front
	light.Angle = Cfg.ATMO.TORCH_ANGLE
	light.Range = Cfg.ATMO.TORCH_RANGE
	light.Brightness = Cfg.ATMO.TORCH_BRIGHTNESS
	light.Color = Cfg.ATMO.TORCH_COLOR
	light.Shadows = true
	light.Enabled = false
	light.Parent = p

	s.torch = p
	s.weld = weld
end

--------------------------------------------------------------------------------
-- HumanBody owns only the invisible R15 gameplay skeleton. BLACKSITE's custom
-- soldier mesh is created on each client and driven from these replicated joints;
-- the server keeps movement, gameplay state and anatomical hitboxes authoritative.
--------------------------------------------------------------------------------
local function onCharacter(plr: Player, char: Model)
	local s = state(plr)
	s.bleeding = false
	s.pitch = 0
	s.stance = "still"
	s.limbs = freshLimbs()
	s.limbBand = {}
	s.anatomy = Anatomy.newState()

	local hum = char:WaitForChild("Humanoid") :: Humanoid
	hum.MaxHealth = Cfg.HEALTH.MAX
	hum.Health = Cfg.HEALTH.MAX
	hum.BreakJointsOnDeath = false
	hum.DisplayDistanceType = Enum.HumanoidDisplayDistanceType.None
	hum.HealthDisplayType = Enum.HumanoidHealthDisplayType.AlwaysOff
	hum.UseJumpPower = true
	hum.JumpPower = 34

	-- Kill Roblox's automatic health regeneration. In this game the only way
	-- back up is a dressing, and there are a fixed number of them.
	task.defer(function()
		local regen = char:FindFirstChild("Health")
		if regen and regen:IsA("Script") then
			regen:Destroy()
		end
	end)

	char:SetAttribute("TorchOn", false)
	char:SetAttribute("LaserOn", false)
	-- Replicated presentation state. Other clients build the same weapon locally
	-- and evaluate this exact authored animation variant/time.
	char:SetAttribute("WeaponId", "MK18")
	char:SetAttribute("WeaponSuppressed", false)
	char:SetAttribute("WeaponADS", false)
	char:SetAttribute("WeaponLean", 0)
	char:SetAttribute("WeaponStance", "still")
	char:SetAttribute("LookPitch", 0)
	char:SetAttribute("WeaponAnimName", "")
	char:SetAttribute("WeaponAnimTime", 0)
	char:SetAttribute("WeaponAnimScale", 1)
	char:SetAttribute("WeaponAnimSerial", 0)
	char:SetAttribute("WeaponShotSerial", 0)
	plr:SetAttribute("Cells", s.cells)
	plr:SetAttribute("Bandages", s.bandages)
	plr:SetAttribute("Bleeding", false)
	for group, maximum in Cfg.HEALTH.LIMB_MAX do
		char:SetAttribute("Limb_" .. group, maximum)
		plr:SetAttribute("Limb_" .. group, maximum)
	end

	HumanBody.configure(char, hum, tostring(plr:GetAttribute("BlacksiteBodySource") or "fallback"))
	HumanBody.buildTacticalGear(char)
	syncAnatomy(plr, char, s)
	buildTorch(char, s)

	hum.Died:Connect(function()
		s.bleeding = false
		plr:SetAttribute("Bleeding", false)
		-- Cells you were carrying are lost where you fell.
		if s.cells > 0 then
			s.cells = 0
			plr:SetAttribute("Cells", 0)
			rMessage:FireClient(plr, "You dropped the cells.", 4)
		end
		rEffect:FireClient(plr, "death")

		-- Turning CharacterAutoLoads off also turns respawning off, so it has
		-- to be driven from here. The arena comes back fast because it is a
		-- test box; dying in the facility is supposed to cost you.
		local wait = ARENA and Cfg.ARENA.RESPAWN or Cfg.RESPAWN_TIME
		task.delay(wait, function()
			if plr.Parent and (not plr.Character or plr.Character == char) then
				loadTacticalCharacter(plr)
			end
		end)
	end)
end

-- Characters are loaded by hand, AFTER the map exists. Both maps create their
-- SpawnLocations at build time, so with CharacterAutoLoads on there is a race
-- where a player who loads first spawns at the world origin and falls past
-- FallenPartsDestroyHeight. It never reliably bit in the facility because the
-- generator takes long enough to build; the arena is fast enough to lose.
Players.CharacterAutoLoads = false

local function admit(plr: Player)
	state(plr)
	plr.CharacterAdded:Connect(function(char)
		onCharacter(plr, char)
	end)
	if plr.Character then
		onCharacter(plr, plr.Character)
	else
		task.spawn(loadTacticalCharacter, plr)
	end
end

Players.PlayerAdded:Connect(admit)
-- Anyone who beat this script to the server -- routine in Studio.
for _, plr in Players:GetPlayers() do
	admit(plr)
end

Players.PlayerRemoving:Connect(function(plr)
	pstate[plr] = nil
end)

--------------------------------------------------------------------------------
-- Damage
--------------------------------------------------------------------------------
local function damagePlayer(char: Model, amount: number, hitPartOrName: any?, hitPosition: Vector3?, ballisticTrauma: number?)
	local hum = char:FindFirstChildOfClass("Humanoid")
	if not hum or hum.Health <= 0 then return end

	local plr = Players:GetPlayerFromCharacter(char)
	if not plr then
		hum:TakeDamage(amount)
		return
	end

	local s = state(plr)
	local hitPart: BasePart? = nil
	if typeof(hitPartOrName) == "Instance" and hitPartOrName:IsA("BasePart") then
		hitPart = hitPartOrName
	elseif typeof(hitPartOrName) == "string" then
		local found = char:FindFirstChild(hitPartOrName, true)
		if found and found:IsA("BasePart") then hitPart = found end
	end
	if not hitPart then
		local fallback = char:FindFirstChild("UpperTorso") or char:FindFirstChild("Torso")
		if fallback and fallback:IsA("BasePart") then hitPart = fallback end
	end

	local group = Cfg.HEALTH.LIMB_GROUPS[hitPart and hitPart.Name or "UpperTorso"] or "Torso"
	local result = nil
	if hitPart then
		result = Anatomy.applyHit(
			s.anatomy,
			hitPart,
			hitPosition or hitPart.Position,
			amount,
			ballisticTrauma or amount,
			math.random()
		)
		group = result.limb or group
	end

	local immediate = result and result.immediateDamage or amount
	if result and result.fatal then
		hum.Health = 0
	else
		hum:TakeDamage(math.clamp(immediate, 0, Cfg.HEALTH.MAX))
	end

	-- Retain the original six-region pools for compatibility with movement,
	-- recoil and the silhouette HUD. The anatomical state is the detailed source
	-- of truth layered underneath these broad functional regions.
	local maximum = Cfg.HEALTH.LIMB_MAX[group] or 100
	local before = s.limbs[group] or maximum
	local after = math.max(0, before - amount)
	s.limbs[group] = after
	char:SetAttribute("Limb_" .. group, after)
	plr:SetAttribute("Limb_" .. group, after)

	local ratio = after / math.max(maximum, 1)
	local band = ratio <= Cfg.HEALTH.LIMB_CRITICAL and 2
		or (ratio <= Cfg.HEALTH.LIMB_WOUNDED and 1 or 0)
	local oldBand = s.limbBand[group] or 0
	if band > oldBand then
		s.limbBand[group] = band
		local readable = (group:gsub("(%l)(%u)", "%1 %2")):lower()
		rMessage:FireClient(plr, band == 2 and ("Your " .. readable .. " is badly damaged.")
			or ("Your " .. readable .. " is wounded."), 3.2)
	end

	if result then
		if result.fractureChanged then
			local def = Anatomy.BONES[result.bone]
			local severity = ({"cracked", "fractured", "shattered"})[result.fracture] or "damaged"
			rMessage:FireClient(plr, (def and def.label or result.bone) .. " " .. severity .. ".", 4.2)
		end
		if result.organCritical then
			local def = result.organ and Anatomy.ORGANS[result.organ]
			rMessage:FireClient(plr, "Critical " .. (def and def.label or "organ") .. " trauma.", 4.2)
		end
	end

	syncAnatomy(plr, char, s)
	rEffect:FireClient(plr, "hurt", immediate, group)

	local rootPart = char.PrimaryPart
	if rootPart then playAt(Cfg.SFX.HURT, rootPart.Position, 60) end
end

-- No entity in the arena. The whole point of that mode is to work on PVP
-- without an unkillable monster interrupting it.
if not ARENA then
	EntityAI.start(map, {
		damagePlayer = damagePlayer,
		onStateChange = function(newState)
			-- Clients use this for the dread mix; they can also just read the
			-- entity's position, so this is only a hint.
			gameState:SetAttribute("EntityState", newState)
		end,
	})
	EntityAI.setPowered(false)
end

--------------------------------------------------------------------------------
-- Material acoustics
--------------------------------------------------------------------------------
local function impactSoundFor(instance: Instance): any
	if not instance:IsA("BasePart") then
		return Cfg.SFX.IMPACT_HARD
	end
	local material = instance.Material
	if material == Enum.Material.Metal or material == Enum.Material.DiamondPlate or material == Enum.Material.CorrodedMetal then
		return Cfg.SFX.IMPACT_METAL
	elseif material == Enum.Material.Wood or material == Enum.Material.WoodPlanks then
		return Cfg.SFX.IMPACT_WOOD
	elseif material == Enum.Material.Glass or material == Enum.Material.Ice then
		return Cfg.SFX.IMPACT_GLASS
	elseif material == Enum.Material.Grass or material == Enum.Material.Ground or material == Enum.Material.Mud or material == Enum.Material.Sand then
		return Cfg.SFX.IMPACT_DIRT
	elseif material == Enum.Material.Concrete or material == Enum.Material.Brick or material == Enum.Material.Rock or material == Enum.Material.Slate then
		return Cfg.SFX.IMPACT_CONCRETE
	end
	return Cfg.SFX.IMPACT_HARD
end

--------------------------------------------------------------------------------
-- Shooting
--------------------------------------------------------------------------------
local function impactEffect(pos: Vector3, normal: Vector3)
	local hole = Instance.new("Part")
	hole.Size = Vector3.new(0.22, 0.22, 0.06)
	hole.Anchored = true
	hole.CanCollide = false
	hole.CanQuery = false
	hole.CanTouch = false
	hole.Color = Color3.fromRGB(12, 12, 12)
	hole.Material = Enum.Material.Slate
	hole.CFrame = CFrame.lookAt(pos + normal * 0.03, pos + normal)
	hole.Parent = workspace

	local spark = Instance.new("PointLight")
	spark.Brightness = 2
	spark.Range = 6
	spark.Color = Color3.fromRGB(255, 190, 120)
	spark.Parent = hole
	task.delay(0.06, function()
		spark.Enabled = false
	end)

	task.delay(Cfg.BALLISTICS.IMPACT_LIFETIME, function()
		hole:Destroy()
	end)
end

rFire.OnServerEvent:Connect(function(plr, origin, dirs, weaponId, suppressed)
	local char = plr.Character
	if not char then
		return
	end
	local hum = char:FindFirstChildOfClass("Humanoid")
	if not hum or hum.Health <= 0 then
		return
	end
	local head = char:FindFirstChild("Head") :: BasePart?
	if not head then
		return
	end

	-- Shape checks before anything is trusted. The id is matched against the
	-- weapon whitelist rather than indexed straight into the module, so a
	-- crafted packet cannot name one of the module's helper functions.
	if typeof(weaponId) ~= "string" or not table.find(Weapons.ALL, weaponId) then
		return
	end
	local w = Weapons[weaponId]
	if typeof(origin) ~= "Vector3" or typeof(dirs) ~= "table" or #dirs < 1 or #dirs > w.pellets then
		return
	end
	if (origin - head.Position).Magnitude > Cfg.LIMITS.MAX_ORIGIN_ERROR then
		return
	end

	-- Rate limit: a sliding one-second window of shot timestamps.
	local s = state(plr)
	local now = os.clock()
	local kept = {}
	for _, t in s.shots do
		if now - t < 1 then
			table.insert(kept, t)
		end
	end
	if #kept >= Cfg.LIMITS.MAX_SHOTS_PER_SEC then
		return
	end
	table.insert(kept, now)
	s.shots = kept
	char:SetAttribute("WeaponId", weaponId)
	char:SetAttribute("WeaponSuppressed", suppressed == true)
	char:SetAttribute("WeaponShotSerial", (tonumber(char:GetAttribute("WeaponShotSerial")) or 0) + 1)

	local ignore = { char }
	local entityModel = EntityAI.getModel()

	----------------------------------------------------------------------------
	-- SUPPRESSION. Anyone the round passes close to gets told, so a near miss is
	-- something you feel rather than something you only infer from not dying.
	-- Perpendicular distance from the shot line to each other player's head,
	-- rejecting anyone behind the muzzle so shots going the other way do not
	-- suppress the people stood next to you.
	----------------------------------------------------------------------------
	do
		local dir0 = (dirs[1] :: Vector3)
		if typeof(dir0) == "Vector3" and dir0.Magnitude > 0.01 then
			local u = dir0.Unit
			local range = Cfg.SCREENFX.SUPPRESS_RANGE
			for _, other in Players:GetPlayers() do
				local ochar = other ~= plr and other.Character
				local ohead = ochar and ochar:FindFirstChild("Head") :: BasePart?
				if ohead then
					local rel = ohead.Position - origin
					local along = rel:Dot(u)
					if along > 0 and along < Cfg.BALLISTICS.MAX_RANGE then
						local perp = (rel - u * along).Magnitude
						if perp < range then
							rEffect:FireClient(other, "suppress", 1 - perp / range)
						end
					end
				end
			end
		end
	end

	for _, dir in dirs do
		if typeof(dir) ~= "Vector3" or dir.Magnitude < 0.01 then
			continue
		end
		-- Scoped per pellet, not per trigger pull: a round that punches through
		-- an arm into the chest must not be counted twice, but nine pellets of
		-- buckshot are nine separate rounds and all of them should hurt.
		local damagedByThisRound: { [Model]: boolean } = {}
		local hits = Ballistics.fire(origin, dir.Unit, ignore, w.damage >= 30)
		for _, hit in hits do
			local victim, vhum = Ballistics.characterFrom(hit.instance)

			if victim and vhum then
				if entityModel and victim == entityModel then
					local dmg = w.damage * hit.damageScale
					if hit.instance.Name == "Head" then
						dmg *= 2.2
					end
					EntityAI.damage(dmg, origin)
					-- No hitmarker, by design. The wet impact sound is the only
					-- confirmation you get that you connected.
					playAt(Cfg.SFX.FLESH, hit.position, 70)
				elseif victim ~= char then
					-- Friendly fire is on. This is a tactical shooter.
					local mult = Cfg.HEALTH.PARTS[hit.instance.Name] or Cfg.HEALTH.DEFAULT_PART_MULT
					if not damagedByThisRound[victim] then
						damagedByThisRound[victim] = true
						damagePlayer(
							victim,
							w.damage * hit.damageScale * mult,
							hit.instance,
							hit.position,
							w.damage * hit.damageScale
						)
					end
					playAt(Cfg.SFX.FLESH, hit.position, 70)
				end
			else
				impactEffect(hit.position, hit.normal)
				playAt(impactSoundFor(hit.instance), hit.position, 90)
				EntityAI.hear(hit.position, Cfg.NOISE.IMPACT)
			end

			-- A round only keeps going if it went through scenery.
			if victim then
				break
			end
		end
	end

	EntityAI.hear(origin, suppressed and Cfg.NOISE.GUNSHOT_SUPPRESSED or Cfg.NOISE.GUNSHOT)

	-- The shooter's own report is played locally the instant they pull the
	-- trigger; everyone else gets it from here, positioned at the muzzle. Doing
	-- it this way avoids the shooter hearing their own shot twice, once late.
	for _, other in Players:GetPlayers() do
		if other ~= plr then
			rEffect:FireClient(other, "shot", origin, w.shotSfx, suppressed == true)
		end
	end
end)

--------------------------------------------------------------------------------
-- Sync: look pitch (aims the helmet light) + stance (drives movement noise)
--------------------------------------------------------------------------------
local VALID_STANCE = { still = true, slow = true, crouch = true, walk = true, sprint = true }

rSync.OnServerEvent:Connect(function(plr, pitch, stance, weaponId, suppressed, ads, lean, animName, animTime, animScale, animSerial)
	if typeof(pitch) ~= "number" or pitch ~= pitch then return end
	local s = state(plr)
	s.pitch = math.clamp(pitch, -Cfg.CAM.PITCH_LIMIT, Cfg.CAM.PITCH_LIMIT)
	if typeof(stance) == "string" and VALID_STANCE[stance] then s.stance = stance end
	if s.weld then
		s.weld.C0 = CFrame.new(0, -0.1, -0.4) * CFrame.Angles(s.pitch, 0, 0)
	end
	local char = plr.Character
	if not char then return end
	char:SetAttribute("LookPitch", s.pitch)
	char:SetAttribute("WeaponStance", s.stance)
	if typeof(weaponId) == "string" and table.find(Weapons.ALL, weaponId) then
		char:SetAttribute("WeaponId", weaponId)
	end
	if typeof(suppressed) == "boolean" then char:SetAttribute("WeaponSuppressed", suppressed) end
	if typeof(ads) == "boolean" then char:SetAttribute("WeaponADS", ads) end
	if typeof(lean) == "number" and lean == lean then
		char:SetAttribute("WeaponLean", math.clamp(lean, -1, 1))
	end
	if typeof(animName) == "string" and #animName <= 64 then char:SetAttribute("WeaponAnimName", animName) end
	if typeof(animTime) == "number" and animTime == animTime then char:SetAttribute("WeaponAnimTime", math.clamp(animTime, 0, 12)) end
	if typeof(animScale) == "number" and animScale == animScale then char:SetAttribute("WeaponAnimScale", math.clamp(animScale, .05, 8)) end
	if typeof(animSerial) == "number" and animSerial == animSerial then char:SetAttribute("WeaponAnimSerial", math.floor(math.clamp(animSerial, 0, 1e9))) end
end)

rGear.OnServerEvent:Connect(function(plr, kind, on)
	local char = plr.Character
	local s = state(plr)
	if not char or typeof(on) ~= "boolean" then
		return
	end
	if kind == "torch" then
		char:SetAttribute("TorchOn", on)
		if s.torch then
			local beam = s.torch:FindFirstChild("Beam") :: SpotLight?
			if beam then
				beam.Enabled = on
			end
		end
	elseif kind == "laser" then
		char:SetAttribute("LaserOn", on)
	end
end)

rNoise.OnServerEvent:Connect(function(plr, position, radius)
	local char = plr.Character
	local hrp = char and char.PrimaryPart
	if not hrp or typeof(position) ~= "Vector3" or typeof(radius) ~= "number" then
		return
	end
	-- Only accept noises the player could plausibly have made.
	if (position - hrp.Position).Magnitude > 12 then
		return
	end
	EntityAI.hear(position, math.clamp(radius, 0, Cfg.NOISE.GUNSHOT))
end)

--------------------------------------------------------------------------------
-- Medical
--------------------------------------------------------------------------------
rBandage.OnServerEvent:Connect(function(plr)
	local char = plr.Character
	local hum = char and char:FindFirstChildOfClass("Humanoid")
	local s = state(plr)
	if not hum or hum.Health <= 0 or s.bandages <= 0 then return end

	local treated, stopped = Anatomy.applyDressing(s.anatomy)
	if not treated then
		rMessage:FireClient(plr, "No external wound to dress.", 2.4)
		return
	end

	s.bandages -= 1
	hum.Health = math.min(hum.MaxHealth, hum.Health + Cfg.HEALTH.DRESSING_HEAL)
	plr:SetAttribute("Bandages", s.bandages)
	syncAnatomy(plr, char, s)
	local _, _, internal = Anatomy.totalBleed(s.anatomy)
	if internal > 0.5 then
		rMessage:FireClient(plr, "External bleeding controlled. Internal bleeding continues.", 4.0)
	else
		rMessage:FireClient(plr, ("Dressing applied — %.0f mL/s controlled."):format(stopped), 2.8)
	end
end)

--------------------------------------------------------------------------------
-- Objectives
--------------------------------------------------------------------------------
local function broadcast(text: string, duration: number?)
	for _, p in Players:GetPlayers() do
		rMessage:FireClient(p, text, duration or 4)
	end
end

local function restorePower()
	if powered then
		return
	end
	powered = true
	gameState:SetAttribute("Powered", true)
	EntityAI.setPowered(true)

	for _, entry in map.lights do
		if not entry.emergency then
			entry.light.Enabled = true
			entry.light.Brightness = Cfg.MAP.LIGHT_BRIGHTNESS -- prelit ones come up to full
			entry.element.Transparency = 0
		end
	end

	Lighting.Ambient = Cfg.ATMO.AMBIENT_POWERED
	Lighting.OutdoorAmbient = Cfg.ATMO.AMBIENT_POWERED
	Lighting.FogEnd = Cfg.ATMO.FOG_END_POWERED
	-- Pull the exposure back once every fixture in the place is lit, or the
	-- restored facility blows out. This is the same knob, used the other way.
	Lighting.ExposureCompensation = Cfg.ATMO.EXPOSURE_POWERED

	local genPos = map.generator.Position
	playAt(Cfg.SFX.POWER_ON, genPos, 600)
	EntityAI.hear(genPos, Cfg.NOISE.POWER_ON)
	broadcast("Main bus live. It heard that. Get to the lift.", 6)
end

local function extract(plr: Player)
	local s = state(plr)
	if s.extracted then
		return
	end
	s.extracted = true
	gameState:SetAttribute("Extracted", (gameState:GetAttribute("Extracted") :: number) + 1)
	rEffect:FireClient(plr, "extract")
	playAt(Cfg.SFX.EXTRACT, map.exit.Position, 120)
	broadcast(plr.Name .. " is out.", 4)

	task.delay(3.5, function()
		local char = plr.Character
		if char and char.PrimaryPart then
			char:PivotTo(CFrame.new(0, 4000, 0))
		end
	end)
end

rInteract.OnServerEvent:Connect(function(plr, thing)
	local char = plr.Character
	local hrp = char and char.PrimaryPart
	if not hrp or typeof(thing) ~= "Instance" or not thing:IsA("BasePart") then
		return
	end
	local tag = thing:GetAttribute("Interact")
	if not tag then
		return
	end
	if (thing.Position - hrp.Position).Magnitude > 14 then
		return
	end

	local s = state(plr)

	if tag == "FUSE" then
		s.cells += 1
		plr:SetAttribute("Cells", s.cells)
		playAt(Cfg.SFX.PICKUP, thing.Position, 60)
		EntityAI.hear(thing.Position, Cfg.NOISE.OBJECTIVE)
		rMessage:FireClient(plr, "Power cell secured. Something heard the rack.", 4)
		thing:Destroy()
	elseif tag == "GENERATOR" then
		if s.cells <= 0 then
			rMessage:FireClient(plr, "Empty slots. The cells are elsewhere.", 3)
			return
		end
		cellsInserted += s.cells
		s.cells = 0
		plr:SetAttribute("Cells", 0)
		gameState:SetAttribute("CellsInserted", math.min(cellsInserted, cellsTotal))
		playAt(Cfg.SFX.PICKUP, thing.Position, 80)
		if cellsInserted >= cellsTotal then
			task.delay(1.2, restorePower)
		else
			rMessage:FireClient(plr, ("Seated. %d of %d."):format(cellsInserted, cellsTotal), 3)
		end
	elseif tag == "EXIT" then
		if not powered then
			rMessage:FireClient(plr, "Lift's dead. No power on the bus.", 3)
			return
		end
		extract(plr)
	elseif tag == "DOOR" then
		local door = thing.Parent
		if door and door:IsA("Model") then
			setDoor(door, not door:GetAttribute("Closed"))
		end
	end
end)

--------------------------------------------------------------------------------
-- Continuous loops
--------------------------------------------------------------------------------

-- Movement noise. The server samples what players are actually doing rather
-- than trusting a client "I made a noise" message. Nothing is listening in the
-- arena, so the whole loop is skipped there.
task.spawn(function()
	while not ARENA do
		task.wait(Cfg.NOISE.SAMPLE_RATE)
		for _, plr in Players:GetPlayers() do
			local char = plr.Character
			local hrp = char and char.PrimaryPart
			local hum = char and char:FindFirstChildOfClass("Humanoid")
			if hrp and hum and hum.Health > 0 then
				local speed = Vector3.new(
					hrp.AssemblyLinearVelocity.X,
					0,
					hrp.AssemblyLinearVelocity.Z
				).Magnitude
				if speed > 1.5 then
					local s = state(plr)
					local radius = Cfg.NOISE.WALK
					if s.stance == "sprint" then
						radius = Cfg.NOISE.SPRINT
					elseif s.stance == "slow" then
						radius = Cfg.NOISE.SLOW
					elseif s.stance == "crouch" then
						radius = Cfg.NOISE.CROUCH
					end
					EntityAI.hear(hrp.Position, radius)
				end
			end
		end
	end
end)

-- Anatomical trauma. Blood loss, pain and shock are simulated separately
-- from Humanoid health. Dressings stop external wounds only; fractures and
-- internal bleeding remain until the player dies or the mission ends.
task.spawn(function()
	while true do
		local dt = Cfg.HEALTH.ANATOMY_TICK
		task.wait(dt)
		for plr, s in pstate do
			local char = plr.Character
			local hum = char and char:FindFirstChildOfClass("Humanoid")
			if hum and hum.Health > 0 and s.anatomy then
				local loss, fatal = Anatomy.tick(s.anatomy, dt)
				if fatal then hum.Health = 0
				elseif loss > 0 then hum:TakeDamage(loss) end
				syncAnatomy(plr, char, s)
			else
				s.bleeding = false
			end
		end
	end
end)

gameState:SetAttribute("Mode", Cfg.MODE)

if ARENA then
	print(("[BLACKSITE] ARENA online -- %d studs, %d spawns, daylight, no entity. Cfg.MODE to switch.")
		:format(Cfg.ARENA.SIZE, #map.spawns))
else
	print(("[BLACKSITE] Facility online. %d rooms, %d cells, %d doors.")
		:format(#map.rooms, cellsTotal, #map.doors))
end
