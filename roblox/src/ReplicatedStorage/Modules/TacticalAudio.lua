--!nonstrict
-- TacticalAudio -- grounded layered one-shot mixer.
--
-- Roblox cannot synthesise arbitrary audio and this project ships no uploaded
-- sound assets, so realism has to come from timing, dynamics and environment
-- treatment rather than from one magic sample. The mixer separates transient,
-- pressure body, early reflection, late tail, mechanism and equipment layers;
-- measures the room around the emitter; applies occlusion for world sounds; and
-- keeps pitch variation coherent across a single event.

local SoundService = game:GetService("SoundService")
local ContentProvider = game:GetService("ContentProvider")
local Workspace = game:GetService("Workspace")

local Audio = {}
local rng = Random.new()

local SPEED_OF_SOUND_STUDS = 343 / 0.35 -- project scale: 1 stud = 0.35 m
local MAX_LIFETIME = 8

local groups: { [string]: SoundGroup } = {}

local function ensureGroup(name: string, volume: number): SoundGroup
	local existing = SoundService:FindFirstChild(name)
	if existing and existing:IsA("SoundGroup") then
		groups[name] = existing
		return existing
	end
	local group = Instance.new("SoundGroup")
	group.Name = name
	group.Volume = volume
	group.Parent = SoundService
	groups[name] = group
	return group
end

local function ensureGroups()
	if groups.Close then
		return
	end
	-- Store semantic aliases as well as Roblox instance names. The previous
	-- pass only populated groups[instance.Name], so groupFor returned nil and
	-- every Sound assignment could fail at runtime before the layer played.
	groups.Close = ensureGroup("BLACKSITE_WeaponClose", 1.0)
	groups.Tail = ensureGroup("BLACKSITE_WeaponTail", 0.92)
	groups.Mechanism = ensureGroup("BLACKSITE_Mechanism", 0.82)
	groups.Foley = ensureGroup("BLACKSITE_Foley", 0.78)
	groups.World = ensureGroup("BLACKSITE_World", 0.94)
end

local function groupFor(role: string?, world: boolean): SoundGroup
	ensureGroups()
	if world then
		return groups.World
	end
	if role == "tail" or role == "reflection" then
		return groups.Tail
	elseif role == "mechanism" then
		return groups.Mechanism
	elseif role == "foley" then
		return groups.Foley
	end
	return groups.Close
end

local function applyProfile(sound: Sound, profile: any, occluded: boolean, enclosure: number, roomSize: number)
	if profile.eq or occluded then
		local eq = Instance.new("EqualizerSoundEffect")
		local data = profile.eq or {}
		eq.LowGain = (data.low or 0) + (occluded and 2 or 0)
		eq.MidGain = (data.mid or 0) + (occluded and -3 or 0)
		eq.HighGain = (data.high or 0) + (occluded and -18 or 0)
		eq.Parent = sound
	end

	if profile.compress then
		local c = profile.compress
		local comp = Instance.new("CompressorSoundEffect")
		comp.Attack = c.attack or 0.018
		comp.Release = c.release or 0.16
		comp.Ratio = c.ratio or 5
		comp.Threshold = c.threshold or -12
		comp.GainMakeup = c.makeup or 0
		comp.Parent = sound
	end

	if profile.role == "tail" or profile.role == "reflection" or profile.reverb then
		local supplied = profile.reverb or {}
		local rv = Instance.new("ReverbSoundEffect")
		-- Short rooms produce dense early reflections; large rooms produce a longer,
		-- thinner tail. Enclosure controls wetness, room size controls decay.
		rv.DecayTime = supplied.decay or math.clamp(0.20 + roomSize / 42, 0.24, 1.55)
		rv.Density = supplied.density or math.clamp(0.48 + enclosure * 0.48, 0, 1)
		rv.Diffusion = supplied.diffusion or math.clamp(0.56 + enclosure * 0.38, 0, 1)
		rv.DryLevel = supplied.dry or -10
		-- Open exterior: almost dry. Corridor/room: dense but still below the
		-- direct report. This avoids the doubled-shot/phasing of the old mixer.
		rv.WetLevel = supplied.wet or (-22 + enclosure * 13)
		rv.Parent = sound
	end

	if profile.distort then
		local d = Instance.new("DistortionSoundEffect")
		d.Level = profile.distort
		d.Parent = sound
	end
end

local function rayParams(exclude: { Instance }?): RaycastParams
	local params = RaycastParams.new()
	params.FilterType = Enum.RaycastFilterType.Exclude
	params.FilterDescendantsInstances = exclude or {}
	params.IgnoreWater = true
	return params
end

local function probeEnvironment(position: Vector3, exclude: { Instance }?): (number, number)
	local params = rayParams(exclude)
	local directions = {
		Vector3.new(34, 0, 0), Vector3.new(-34, 0, 0),
		Vector3.new(0, 34, 0), Vector3.new(0, -18, 0),
		Vector3.new(0, 0, 34), Vector3.new(0, 0, -34),
	}
	local blocked = 0
	local distanceSum = 0
	for _, direction in directions do
		local hit = Workspace:Raycast(position, direction, params)
		if hit then
			blocked += 1
			distanceSum += hit.Distance
		else
			distanceSum += direction.Magnitude
		end
	end
	local enclosure = blocked / #directions
	local roomSize = distanceSum / #directions
	return enclosure, roomSize
end

local function isOccluded(listener: Vector3, emitter: Vector3, exclude: { Instance }?): boolean
	local delta = emitter - listener
	if delta.Magnitude < 1 then
		return false
	end
	return Workspace:Raycast(listener, delta, rayParams(exclude)) ~= nil
end

local function makeHolder(position: Vector3): BasePart
	local p = Instance.new("Part")
	p.Name = "AudioEmitter"
	p.Anchored = true
	p.CanCollide = false
	p.CanQuery = false
	p.CanTouch = false
	p.Transparency = 1
	p.Size = Vector3.one * 0.05
	p.CFrame = CFrame.new(position)
	p.Parent = Workspace
	return p
end

local function selectId(layer: any): string
	if layer.variants and #layer.variants > 0 then
		return layer.variants[rng:NextInteger(1, #layer.variants)]
	end
	return layer.id
end

local function emitLayer(
	layer: any,
	parent: Instance,
	world: boolean,
	pitchScale: number,
	volumeScale: number,
	occluded: boolean,
	enclosure: number,
	roomSize: number
)
	local sound = Instance.new("Sound")
	sound.Name = "TacticalOneShot"
	sound.SoundId = selectId(layer)
	sound.Volume = math.max(0, (layer.vol or 1) * volumeScale * (occluded and 0.62 or 1))
	sound.PlaybackSpeed = math.max(0.05, (layer.pitch or 1) * pitchScale)
	sound.SoundGroup = groupFor(layer.role, world)
	if world then
		sound.RollOffMode = Enum.RollOffMode.InverseTapered
		sound.RollOffMinDistance = layer.minDistance or 8
		sound.RollOffMaxDistance = layer.maxDistance or 420
		sound.EmitterSize = layer.emitterSize or 5
	end
	applyProfile(sound, layer, occluded, enclosure, roomSize)
	sound.Parent = parent
	if layer.startAt and layer.startAt > 0 then
		pcall(function()
			sound.TimePosition = layer.startAt
		end)
	end
	sound:Play()
	local lifetime = layer.cut or layer.life or MAX_LIFETIME
	task.delay(lifetime, function()
		if sound.Parent then
			sound:Stop()
			sound:Destroy()
		end
	end)
end

local function layersOf(def: any): { any }
	if def.layers then
		return def.layers
	end
	return { def }
end

local function play(
	def: any,
	parent: Instance,
	world: boolean,
	position: Vector3,
	jitter: number?,
	exclude: { Instance }?
)
	if not def then
		return
	end
	local enclosure, roomSize = probeEnvironment(position, exclude)
	local listener = Workspace.CurrentCamera and Workspace.CurrentCamera.CFrame.Position or position
	local occluded = world and isOccluded(listener, position, exclude) or false
	local coherentPitch = jitter and rng:NextNumber(1 - jitter, 1 + jitter) or 1

	for _, sourceLayer in layersOf(def) do
		local layer = table.clone(sourceLayer)
		local delay = layer.delay or 0
		local volumeScale = 1

		if layer.role == "reflection" then
			-- First bounce: travel to a nearby wall and back. This is only a few
			-- milliseconds in a corridor but materially changes the perceived room.
			delay += math.clamp((roomSize * 2) / SPEED_OF_SOUND_STUDS, 0.008, 0.075)
			volumeScale *= 0.38 + enclosure * 0.74
		elseif layer.role == "tail" then
			delay += math.clamp((roomSize * 2.8) / SPEED_OF_SOUND_STUDS, 0.014, 0.11)
			volumeScale *= 0.28 + enclosure * 1.02
		elseif layer.role == "mechanism" then
			volumeScale *= world and 0.62 or 1
		end

		if layer.randomVolume then
			volumeScale *= rng:NextNumber(1 - layer.randomVolume, 1 + layer.randomVolume)
		end
		local layerPitch = coherentPitch * rng:NextNumber(
			1 - (layer.pitchJitter or 0),
			1 + (layer.pitchJitter or 0)
		)

		local function doEmit()
			if parent.Parent or parent == Workspace.CurrentCamera then
				emitLayer(layer, parent, world, layerPitch, volumeScale, occluded, enclosure, roomSize)
			end
		end
		if delay > 0 then
			task.delay(delay, doEmit)
		else
			doEmit()
		end
	end
end

function Audio.playLocal(def: any, jitter: number?, camera: Camera, exclude: { Instance }?)
	play(def, camera, false, camera.CFrame.Position, jitter, exclude)
end

function Audio.playWorld(def: any, position: Vector3, jitter: number?, exclude: { Instance }?)
	local holder = makeHolder(position)
	play(def, holder, true, position, jitter, exclude)
	task.delay(MAX_LIFETIME + 0.5, function()
		if holder.Parent then
			holder:Destroy()
		end
	end)
end

function Audio.preload(definitions: any)
	local sounds = {}
	local seen = {}
	local function collect(def: any)
		if type(def) ~= "table" then
			return
		end
		for _, layer in layersOf(def) do
			local id = layer.id
			if type(id) == "string" and id ~= "" and not seen[id] then
				seen[id] = true
				local sound = Instance.new("Sound")
				sound.SoundId = id
				sound.Volume = 0
				sound.Parent = SoundService
				table.insert(sounds, sound)
			end
		end
	end
	for _, def in definitions do
		collect(def)
	end
	if #sounds > 0 then
		pcall(function()
			ContentProvider:PreloadAsync(sounds)
		end)
	end
	for _, sound in sounds do
		sound:Destroy()
	end
end

return Audio
