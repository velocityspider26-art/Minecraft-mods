--!nonstrict
-- Atmosphere -- darkness, night vision, batteries, faulty lights, and dread.
--
-- Split out from FPSClient because none of it is about weapon handling and all
-- of it runs at a much lower rate. The two scripts share nothing directly;
-- they communicate through replicated attributes, which is also how a late
-- joiner gets a correct world without any handshake.
--
-- The dread system is the horror equivalent of a hit indicator: it reads the
-- entity's real position out of Workspace and turns proximity into a heartbeat
-- and a closing vignette. It never lies and it never triggers on nothing.

local Players = game:GetService("Players")
local Lighting = game:GetService("Lighting")
local RunService = game:GetService("RunService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local UserInputService = game:GetService("UserInputService")
local SoundService = game:GetService("SoundService")

local player = Players.LocalPlayer

local Modules = ReplicatedStorage:WaitForChild("Modules")
local Cfg = require(Modules:WaitForChild("GameConfig"))
local Hud = require(Modules:WaitForChild("Hud"))
local Fx = require(Modules:WaitForChild("ScreenFx"))

local Net = ReplicatedStorage:WaitForChild("Net")
local rGear = Net:WaitForChild("Gear") :: RemoteEvent
local gameState = ReplicatedStorage:WaitForChild("GameState")

Hud.build()

--------------------------------------------------------------------------------
-- Post-processing (client-created, so it never replicates)
--------------------------------------------------------------------------------
local GFX = Cfg.GFX
local NV = Cfg.NVG
local ARENA = Cfg.MODE == "ARENA"
local DAY = Cfg.ATMO.DAY

-- THE GRADE WAS EATING THE GAME. ColorCorrection.Contrast pivots about 0.5, so
-- the old Brightness -0.025 / Contrast 0.18 evaluated to out = 1.18*in - 0.115,
-- which reaches zero at in = 0.0975: every pixel below 9.75% grey was being
-- clamped to literal black on the way to the screen, after lighting, after
-- tonemapping, unconditionally. Ambient-lit concrete lands around 3%. No
-- fixture brightness and no ambient value could ever have survived that.
local cc = Instance.new("ColorCorrectionEffect")
cc.Name = "BlacksiteGrade"
-- The cold crushed horror grade reads as a broken television in daylight, so
-- the arena gets a near-neutral one.
cc.Brightness = ARENA and DAY.GRADE_BRIGHTNESS or GFX.GRADE_BRIGHTNESS
cc.Contrast = ARENA and DAY.GRADE_CONTRAST or GFX.GRADE_CONTRAST
cc.Saturation = ARENA and DAY.GRADE_SATURATION or GFX.GRADE_SATURATION
cc.TintColor = ARENA and DAY.GRADE_TINT or GFX.GRADE_TINT
cc.Parent = Lighting

-- Gain is NOT here. Brightness on a ColorCorrection is additive, and additive
-- is a black-point lift: it raises empty air and dark surfaces by the same
-- amount, so it can never make a surface read as amplified -- it can only fog
-- the frame. Real gain is multiplicative and lives on
-- Lighting.ExposureCompensation, below. What this effect does is the tube's
-- COLOUR and its terrible dynamic range, which is why Contrast is negative.
local nvgFx = Instance.new("ColorCorrectionEffect")
nvgFx.Name = "NVG"
nvgFx.Enabled = false
nvgFx.Brightness = NV.GAIN
nvgFx.Contrast = NV.CONTRAST
nvgFx.Saturation = -1
nvgFx.TintColor = NV.TINT
nvgFx.Parent = Lighting

local blur = Instance.new("BlurEffect")
local nvgBlur = Instance.new("BlurEffect")
blur.Name = "Pain"
blur.Size = 0
blur.Parent = Lighting
-- Real image intensifiers resolve about 20/40. The softness is global, which
-- also means the barely-visible world outside the tubes is soft -- which is
-- what it looks like when your eye is not behind the glass.
nvgBlur.Name = "NVGAcuity"
nvgBlur.Size = 0
nvgBlur.Parent = Lighting

-- Only actual light sources bloom, not pale concrete: threshold above 1.
local bloom = Instance.new("BloomEffect")
bloom.Name = "Bloom"
bloom.Intensity = GFX.BLOOM_INTENSITY
bloom.Threshold = GFX.BLOOM_THRESHOLD
bloom.Size = GFX.BLOOM_SIZE
bloom.Parent = Lighting

-- Shallow enough to keep the far dark reading as depth rather than as a flat
-- black wall, without turning the whole game into a smear.
local dof = Instance.new("DepthOfFieldEffect")
dof.Name = "Focus"
dof.FarIntensity = GFX.DOF_FAR_INTENSITY
dof.NearIntensity = GFX.DOF_NEAR_INTENSITY
dof.FocusDistance = GFX.DOF_FOCUS
dof.InFocusRadius = GFX.DOF_INFOCUS
dof.Parent = Lighting

local atmo = Lighting:FindFirstChildOfClass("Atmosphere")
if not atmo then
	atmo = Instance.new("Atmosphere")
	atmo.Parent = Lighting
end
;(atmo :: Atmosphere).Density = ARENA and DAY.ATMO_DENSITY or 0.42
;(atmo :: Atmosphere).Haze = ARENA and DAY.ATMO_HAZE or 1.8
;(atmo :: Atmosphere).Glare = ARENA and DAY.ATMO_GLARE or 0
;(atmo :: Atmosphere).Color = ARENA and DAY.ATMO_COLOR or Color3.fromRGB(30, 32, 36)
;(atmo :: Atmosphere).Decay = ARENA and DAY.ATMO_DECAY or Color3.fromRGB(10, 11, 14)

-- Depth of field on a 100 m range just smears the far plates, and the arena is
-- the one place you actually want to see distance clearly.
if ARENA then
	dof.FarIntensity = 0
	dof.NearIntensity = 0
end

--------------------------------------------------------------------------------
-- 2D audio
--------------------------------------------------------------------------------
while not workspace.CurrentCamera do
	task.wait()
end

local function loopSound(def: { id: string, vol: number, pitch: number }): Sound
	local s = Instance.new("Sound")
	s.SoundId = def.id
	s.Volume = 0
	s.PlaybackSpeed = def.pitch
	s.Looped = true
	s.Parent = workspace.CurrentCamera
	s:Play()
	return s
end

local heartbeat = loopSound(Cfg.SFX.HEARTBEAT)
local breathing = loopSound(Cfg.SFX.BREATH)

--------------------------------------------------------------------------------
-- Batteries
--------------------------------------------------------------------------------
local torchBattery = Cfg.ATMO.TORCH_BATTERY
local nvgBattery = Cfg.ATMO.NVG_BATTERY
local nvgOn = false
local torchWarned = false

--------------------------------------------------------------------------------
-- Panoramic multi-tube mask
--
-- Roblox post-processing is full-screen; there is no way to clip a
-- ColorCorrection to a circle. So the amplified image is rendered everywhere
-- and this covers up everything outside the tubes.
--
-- The mask is built from horizontal strips. For each strip we solve the x-span
-- that the configured tubes cover at that height, merge those spans, and darken
-- the complement. Multiple shrinking union passes create a soft panoramic rim
-- without image assets, hard circles, or a seam through the middle.
--------------------------------------------------------------------------------
local nvgGui: ScreenGui
local maskStrips: { { frames: { Frame }, y: number } } = {}
local tubeFrames: { Frame } = {}
local fibreCells: { Frame } = {}
local sparkles: { Frame } = {}
local scintHolder: Frame
local sparkleHolder: Frame

local function buildNvgOverlay()
	nvgGui = Instance.new("ScreenGui")
	nvgGui.Name = "NVGOverlay"
	nvgGui.ResetOnSpawn = false
	nvgGui.IgnoreGuiInset = true
	nvgGui.DisplayOrder = 20
	nvgGui.Enabled = false
	nvgGui.Parent = player:WaitForChild("PlayerGui")

	----------------------------------------------------------------------------
	-- FIBRE BUNDLE -- dense, tiny, faint, STATIC.
	--
	-- The image comes out of a real tube through a fibre-optic bundle, which
	-- leaves a hex cell pattern over everything. Because nothing ever writes to
	-- these after layout, the grid can afford to be a thousand cells and be
	-- genuinely fine-grained. The previous version sized this grid for
	-- affordable per-frame UPDATES, which is why it ended up at 50 px per cell
	-- and read as bubble wrap.
	----------------------------------------------------------------------------
	scintHolder = Instance.new("Frame")
	scintHolder.Name = "FibreBundle"
	scintHolder.BackgroundTransparency = 1
	scintHolder.ZIndex = 3 -- under the mask, so it only shows inside the tubes
	scintHolder.Parent = nvgGui

	----------------------------------------------------------------------------
	-- SCINTILLATION -- sparse, bright, MOVING.
	--
	-- The original jittered a global ColorCorrection.Brightness, which is a
	-- STROBE: the whole frame going light and dark together reads as a failing
	-- lamp. Real scintillation is a handful of bright points appearing and
	-- vanishing at random positions, so these are teleported rather than
	-- modulated -- far fewer instances, far more convincing, and re-rolled at a
	-- fixed rate so it is identical at 30 and 144 fps.
	----------------------------------------------------------------------------
	sparkleHolder = Instance.new("Frame")
	sparkleHolder.Name = "Scintillation"
	sparkleHolder.BackgroundTransparency = 1
	sparkleHolder.ZIndex = 3
	sparkleHolder.Parent = nvgGui

	for _ = 1, NV.SPARKLE_COUNT do
		local dot = Instance.new("Frame")
		dot.Name = "P"
		dot.BackgroundColor3 = NV.SCINT_COLOR
		dot.BackgroundTransparency = 1
		dot.BorderSizePixel = 0
		dot.AnchorPoint = Vector2.new(0.5, 0.5)
		dot.Size = UDim2.fromOffset(NV.SPARKLE_SIZE, NV.SPARKLE_SIZE)
		dot.ZIndex = 3
		dot.Parent = sparkleHolder

		local rc = Instance.new("UICorner")
		rc.CornerRadius = UDim.new(0.5, 0)
		rc.Parent = dot

		table.insert(sparkles, dot)
	end

	-- N circles produce at most N spans on a scanline, so at most N+1 gaps to
	-- cover. Two tubes need three bars per strip -- left, the wedge between
	-- them, and right. Four tubes for GPNVGs would need five, and this
	-- allocates for whatever is in the config.
	-- barsPerStrip * FEATHER_PASSES: each pass needs its own set of bars, since
	-- they are stacked at partial opacity to build the gradient.
	local barsPerStrip = (#NV.TUBES + 1) * math.max(1, NV.FEATHER_PASSES)
	local h = 1 / NV.STRIPS

	for i = 1, NV.STRIPS do
		local y = (i - 1) * h
		local frames = {}
		for _ = 1, barsPerStrip do
			local f = Instance.new("Frame")
			f.BackgroundColor3 = NV.MASK_COLOR
			-- Per-pass opacity: N of these stacked multiply down to roughly
			-- MASK_OPACITY in the region every pass covers.
			f.BackgroundTransparency = (1 - NV.MASK_OPACITY) ^ (1 / math.max(1, NV.FEATHER_PASSES))
			f.BorderSizePixel = 0
			f.Size = UDim2.new(0, 0, h + 0.002, 0) -- slight overlap, no seams
			f.Position = UDim2.new(0, 0, y, 0)
			f.Visible = false
			f.ZIndex = 4
			f.Parent = nvgGui
			table.insert(frames, f)
		end
		maskStrips[i] = { frames = frames, y = y }
	end

	-- One vignette per tube: dense rings across the OUTER band only.
	--
	-- The first version spread 14 rings over the whole radius. At a 360 px
	-- radius that put them 26 px apart with 8 px strokes, so it drew fourteen
	-- separate circles with visible gaps -- a bullseye, not a vignette. The
	-- rings now cover from VIGNETTE_INNER to the rim and there are enough of
	-- them that the strokes overlap into a gradient. Thickness is set in
	-- layout, where the pixel radius is actually known.
	for ti, tube in NV.TUBES do
		local holder = Instance.new("Frame")
		holder.BackgroundTransparency = 1
		holder.AnchorPoint = Vector2.new(0.5, 0.5)
		holder.ZIndex = 5
		holder.Parent = nvgGui
		tubeFrames[#tubeFrames + 1] = holder

		-- Small tube-to-tube gain variance stops the four lobes from reading as
		-- four perfectly duplicated UI circles.
		local bias = (NV.TUBE_BIAS and NV.TUBE_BIAS[ti]) or 0
		if bias ~= 0 then
			local disc = Instance.new("Frame")
			disc.Name = "Bias"
			disc.BackgroundColor3 = bias > 0 and NV.TINT or NV.MASK_COLOR
			disc.BackgroundTransparency = 1 - math.abs(bias)
			disc.BorderSizePixel = 0
			disc.Size = UDim2.fromScale(1, 1)
			disc.Position = UDim2.fromScale(0.5, 0.5)
			disc.AnchorPoint = Vector2.new(0.5, 0.5)
			disc.ZIndex = 2
			disc.Parent = holder

			local dc = Instance.new("UICorner")
			dc.CornerRadius = UDim.new(0.5, 0)
			dc.Parent = disc
		end

		-- 0 skips this entirely. The feathered mask replaces it -- see the
		-- FEATHER_PASSES note in GameConfig.
		local n = NV.VIGNETTE_RINGS
		for ring = 1, n do
			local t = (ring - 1) / (n - 1) -- 0 at the inner edge, 1 at the rim
			local f = NV.VIGNETTE_INNER + (1 - NV.VIGNETTE_INNER) * t

			local circle = Instance.new("Frame")
			circle.Name = "Ring"
			circle.BackgroundTransparency = 1
			circle.Size = UDim2.fromScale(f, f)
			circle.Position = UDim2.fromScale(0.5, 0.5)
			circle.AnchorPoint = Vector2.new(0.5, 0.5)
			circle.ZIndex = 5
			circle.Parent = holder

			local corner = Instance.new("UICorner")
			corner.CornerRadius = UDim.new(0.5, 0)
			corner.Parent = circle

			local stroke = Instance.new("UIStroke")
			-- Every ring the same colour. The outermost used to get EDGE_GLOW,
			-- which drew exactly the fake bright cyan rim the brief complained
			-- about -- and it was still visible in the latest screenshot.
			stroke.Color = NV.MASK_COLOR
			stroke.Transparency = 1 - (t ^ 2.2) * NV.VIGNETTE_DEPTH
			stroke.Parent = circle
		end
	end
end

-- Recomputed whenever the viewport changes, not per frame.
local function layoutNvgOverlay()
	if not nvgGui then
		return
	end
	local vp = workspace.CurrentCamera.ViewportSize
	local sh = vp.Y
	local cx, cy = vp.X * 0.5, vp.Y * 0.5

	for i, tube in NV.TUBES do
		local holder = tubeFrames[i]
		if holder then
			local d = tube.r * 2 * sh
			holder.Size = UDim2.fromOffset(d, d)
			holder.Position = UDim2.fromOffset(cx + tube.x * sh, cy + tube.y * sh)

			-- Stroke thickness has to beat the spacing between rings or the
			-- gradient breaks back into visible circles. Spacing is the band
			-- width divided by the ring count; 1.9x that guarantees overlap.
			if NV.VIGNETTE_RINGS > 0 then
				local band = (1 - NV.VIGNETTE_INNER) * (d * 0.5)
				local thickness = math.max(2, (band / NV.VIGNETTE_RINGS) * 1.9)
				for _, ring in holder:GetChildren() do
					local stroke = ring:FindFirstChildOfClass("UIStroke")
					if stroke then
						stroke.Thickness = thickness
					end
				end
			end
		end
	end

	----------------------------------------------------------------------------
	-- Both noise layers cover only the bounding box of the tubes. Outside it the
	-- mask is doing the work and there is nothing to sparkle on.
	----------------------------------------------------------------------------
	if scintHolder then
		local x0, x1, y0b, y1b = math.huge, -math.huge, math.huge, -math.huge
		for _, tube in NV.TUBES do
			x0 = math.min(x0, cx + (tube.x - tube.r) * sh)
			x1 = math.max(x1, cx + (tube.x + tube.r) * sh)
			y0b = math.min(y0b, cy + (tube.y - tube.r) * sh)
			y1b = math.max(y1b, cy + (tube.y + tube.r) * sh)
		end
		local bw, bh = math.ceil(x1 - x0), math.ceil(y1b - y0b)
		local pos = UDim2.fromOffset(math.floor(x0), math.floor(y0b))
		local size = UDim2.fromOffset(bw, bh)
		scintHolder.Position, scintHolder.Size = pos, size
		sparkleHolder.Position, sparkleHolder.Size = pos, size

		------------------------------------------------------------------------
		-- Rebuild the fibre grid for this viewport. Pitch is in PIXELS so the
		-- cell size is resolution independent, then widened if that would blow
		-- past FIBRE_MAX -- so a 4K screen gets a coarser bundle rather than
		-- eight thousand frames.
		------------------------------------------------------------------------
		local pitch = NV.FIBRE_PITCH
		local cols = math.max(1, math.floor(bw / pitch))
		local rows = math.max(1, math.floor(bh / pitch))
		if not NV.FIBRE then
			cols, rows = 0, 0
		elseif cols * rows > NV.FIBRE_MAX then
			local scale = math.sqrt((cols * rows) / NV.FIBRE_MAX)
			pitch *= scale
			cols = math.max(1, math.floor(bw / pitch))
			rows = math.max(1, math.floor(bh / pitch))
		end

		for _, c in fibreCells do
			c:Destroy()
		end
		table.clear(fibreCells)

		local d = math.max(2, math.floor(pitch * 0.92 + 0.5))
		for row = 1, rows do
			for col = 1, cols do
				local cell = Instance.new("Frame")
				cell.Name = "F"
				cell.BackgroundColor3 = NV.SCINT_COLOR
				cell.BackgroundTransparency = 1 - NV.FIBRE_OPACITY
				cell.BorderSizePixel = 0
				cell.AnchorPoint = Vector2.new(0.5, 0.5)
				cell.Size = UDim2.fromOffset(d, d)
				-- Hex offset: half a cell across on every other row.
				cell.Position = UDim2.fromOffset(
					math.floor((col - 0.5 + (row % 2 == 0 and 0.5 or 0)) * pitch),
					math.floor((row - 0.5) * pitch)
				)
				cell.ZIndex = 3
				cell.Parent = scintHolder

				local rc = Instance.new("UICorner")
				rc.CornerRadius = UDim.new(0.5, 0)
				rc.Parent = cell

				table.insert(fibreCells, cell)
			end
		end
	end

	local spans, merged, bars = {}, {}, {}

	for i, strip in maskStrips do
		-- Whole-pixel boundaries. The first version gave every strip a 0.2%
		-- scale overlap to avoid seams, which meant two 93%-opaque bars
		-- stacking on the overlap and drawing a darker line -- the horizontal
		-- banding across the whole screen. Rounding to integer pixels tiles
		-- exactly: no overlap, no gaps.
		local y0 = math.floor((i - 1) / NV.STRIPS * vp.Y + 0.5)
		local y1 = math.floor(i / NV.STRIPS * vp.Y + 0.5)
		local hPx = math.max(1, y1 - y0)

		-- Sample the middle of the strip and intersect it with every circle.
		local py = ((i - 0.5) / NV.STRIPS) * vp.Y

		------------------------------------------------------------------------
		-- FEATHER. The same span solve, run once per pass at a shrinking radius,
		-- and every pass's complement is drawn at partial opacity. Outside the
		-- largest radius all passes cover, so it reaches full opacity; between
		-- the smallest and largest, fewer and fewer passes reach, which is a
		-- gradient that follows the UNION of the circles exactly.
		--
		-- This is what replaced the per-tube rings, and it is why the tubes can
		-- now overlap into one connected field without a seam: there is no
		-- per-tube geometry left anywhere in the mask.
		------------------------------------------------------------------------
		local passes = math.max(1, NV.FEATHER_PASSES)
		table.clear(bars)

		for pass = 1, passes do
			-- pass 1 is the rim, pass `passes` reaches furthest in.
			local shrink = 1 - NV.FEATHER_DEPTH * ((pass - 1) / passes)
			table.clear(spans)

			for _, tube in NV.TUBES do
				local tcx = cx + tube.x * sh
				local r = tube.r * sh * shrink
				local dy = py - (cy + tube.y * sh)
				if math.abs(dy) < r then
					local hw = math.sqrt(r * r - dy * dy)
					table.insert(spans, { tcx - hw, tcx + hw })
				end
			end
			table.sort(spans, function(a, b)
				return a[1] < b[1]
			end)

			-- Merge overlapping spans, then cover the complement. This is what
			-- fuses two overlapping circles into ONE region instead of leaving a
			-- seam down the middle where they meet.
			table.clear(merged)
			for _, s in spans do
				local last = merged[#merged]
				if last and s[1] <= last[2] then
					last[2] = math.max(last[2], s[2])
				else
					table.insert(merged, { s[1], s[2] })
				end
			end

			local cursor = 0
			for _, s in merged do
				if s[1] > cursor then
					table.insert(bars, { cursor, s[1] })
				end
				cursor = math.max(cursor, s[2])
			end
			if cursor < vp.X then
				table.insert(bars, { cursor, vp.X })
			end
		end

		for j, frame in strip.frames do
			local b = bars[j]
			if b and b[2] > b[1] then
				frame.Visible = true
				frame.Position = UDim2.fromOffset(math.floor(b[1] + 0.5), y0)
				frame.Size = UDim2.fromOffset(math.ceil(b[2]) - math.floor(b[1] + 0.5), hPx)
			else
				frame.Visible = false
			end
		end
	end
end

--------------------------------------------------------------------------------
-- Gain and gating.
--
-- `gate` is 0..1: how hard the tubes are protecting themselves. It is driven
-- off real light sources near and in front of the player, because there is no
-- way to read the framebuffer -- and that is the same signal a real tube is
-- responding to anyway. Attack is fast, release is slow, exactly like an AGC.
--------------------------------------------------------------------------------
local gate, gateTarget = 0, 0

local function applyExposure(powered: boolean)
	local base = ARENA and DAY.EXPOSURE
		or (powered and Cfg.ATMO.EXPOSURE_POWERED or Cfg.ATMO.EXPOSURE)
	if nvgOn then
		-- THE actual amplification. Multiplicative, in stops, applied before
		-- the tonemapper: 2^2.35 is 5.1x the light reaching the screen, and
		-- true black stays exactly black because 0 * 5.1 is still 0. That is
		-- the whole answer to "amplified without lifting the black point",
		-- and it is why GAIN on the ColorCorrection is now 0.02 instead of
		-- 0.33 -- an additive lift was never going to do this job.
		Lighting.ExposureCompensation = base + NV.EXPOSURE - gate * NV.GATE_STRENGTH
	else
		Lighting.ExposureCompensation = base
	end
end

local function setNvg(on: boolean)
	if on and nvgBattery <= 0 then
		Hud.say("Tubes are dead.", 2.5, Hud.COLORS.BAD)
		return
	end
	nvgOn = on
	nvgFx.Enabled = on
	nvgBlur.Size = on and NV.BLUR or 0
	-- The colour grade is BYPASSED under the tubes. Two stacked
	-- ColorCorrections multiply their contrast curves together, and a tube's
	-- image has no dynamic range to give away -- running the game's contrasty
	-- grade underneath the NVG effect was crushing the tube interiors on its
	-- own, on top of everything else.
	cc.Enabled = not on
	-- Gain blows highlights: through tubes a porch light is a white disc, not
	-- a bright lamp. Dropping the bloom threshold is what produces that.
	bloom.Threshold = on and NV.BLOOM_THRESHOLD or GFX.BLOOM_THRESHOLD
	bloom.Intensity = on and NV.BLOOM_INTENSITY or GFX.BLOOM_INTENSITY
	bloom.Size = on and NV.BLOOM_SIZE or GFX.BLOOM_SIZE
	if not on then
		gate, gateTarget = 0, 0
	end
	applyExposure(gameState:GetAttribute("Powered") == true)
	if nvgGui then
		nvgGui.Enabled = on
		if on then
			layoutNvgOverlay()
		end
	end
	Hud.status(on and "NVG DOWN" or "NVG UP")
end

buildNvgOverlay()
layoutNvgOverlay()
workspace.CurrentCamera:GetPropertyChangedSignal("ViewportSize"):Connect(layoutNvgOverlay)

UserInputService.InputBegan:Connect(function(input, processed)
	if processed then
		return
	end
	if input.KeyCode == Enum.KeyCode.N then
		setNvg(not nvgOn)
	end
end)

--------------------------------------------------------------------------------
-- Faulty fixtures
--------------------------------------------------------------------------------
local flickerLights: { { light: Light, element: BasePart?, emergency: boolean } } = {}
-- Every fixture, not just the faulty ones: the NVG gate needs to know when a
-- lit one is close and in front of you.
local allLights: { { light: Light, part: BasePart } } = {}

local function collectLights()
	flickerLights = {}
	allLights = {}
	local facility = workspace:FindFirstChild("Facility")
	local folder = facility and facility:FindFirstChild("Lights")
	if not folder then
		return
	end
	for _, housing in folder:GetChildren() do
		if housing:IsA("BasePart") then
			local any = housing:FindFirstChildOfClass("SpotLight") or housing:FindFirstChildOfClass("PointLight")
			if any then
				table.insert(allLights, { light = any, part = housing })
			end
		end
		if housing:IsA("BasePart") and housing:GetAttribute("Flicker") then
			local light = housing:FindFirstChildOfClass("SpotLight")
			if light then
				table.insert(flickerLights, {
					light = light,
					-- The glowing tube has to blink with the light, or a dead
					-- fixture still reads as lit from across the room.
					element = housing:FindFirstChild("Element") :: BasePart?,
					emergency = housing:GetAttribute("Emergency") == true,
				})
			end
		end
	end
end

task.spawn(function()
	-- The facility is built by the server on start-up; wait for it, then index.
	while not workspace:FindFirstChild("Facility") do
		task.wait(0.25)
	end
	task.wait(1)
	collectLights()
end)

--------------------------------------------------------------------------------
-- Slow loop: flicker, batteries, objective readout
--------------------------------------------------------------------------------
local dread = 0

task.spawn(function()
	while true do
		local dt = task.wait(1 / 12)
		local powered = gameState:GetAttribute("Powered") == true

		-- Faulty fixtures stutter. Only the ones that should be lit at all.
		for _, entry in flickerLights do
			local intended = entry.emergency or powered
			local lit = intended and math.random() > 0.13
			entry.light.Enabled = lit
			if entry.element then
				entry.element.Transparency = lit and 0 or 0.85
			end
		end

		-- Torch battery. The server owns whether the light is on, so read it
		-- back off the character rather than tracking a second copy here.
		local char = player.Character
		local torchOn = char and char:GetAttribute("TorchOn") == true
		if torchOn then
			torchBattery = math.max(0, torchBattery - dt)
			if torchBattery <= 0 then
				rGear:FireServer("torch", false)
				Hud.warn("Light's dead.", 4)
			elseif torchBattery <= Cfg.ATMO.TORCH_BATTERY_WARN and not torchWarned then
				torchWarned = true
				Hud.warn("Light's going yellow.", 4)
			end
		end

		if nvgOn then
			nvgBattery = math.max(0, nvgBattery - dt)
			if nvgBattery <= 0 then
				setNvg(false)
				Hud.warn("Tubes browned out.", 4)
			end
		end

		----------------------------------------------------------------------
		-- Ambient, fog and exposure. Reasserted because the server owns these
		-- properties and re-replicates them when the power comes back.
		--
		-- NVG ambient is now a FRACTION of what it was -- (38,44,44) rather
		-- than (132,152,152) -- because ExposureCompensation does the
		-- amplifying. The old value with +2.35 stops on top of it would clip
		-- the entire frame to white. This is just the skyglow a tube picks up
		-- and your naked eye does not.
		----------------------------------------------------------------------
		-- In the arena the server's daylight is correct and must be left alone;
		-- the only thing that still overrides it is putting the tubes down, and
		-- doing that in daylight SHOULD blind you -- the gate below is what
		-- makes that read as a tube protecting itself rather than a bug.
		if ARENA then
			if nvgOn then
				Lighting.Ambient = NV.AMBIENT
				Lighting.OutdoorAmbient = NV.AMBIENT
			else
				Lighting.Ambient = DAY.AMBIENT
				Lighting.OutdoorAmbient = DAY.OUTDOOR_AMBIENT
			end
		else
			local amb = nvgOn and NV.AMBIENT
				or (powered and Cfg.ATMO.AMBIENT_POWERED or Cfg.ATMO.AMBIENT_DARK)
			Lighting.Ambient = amb
			-- The docs are explicit that effective OutdoorAmbient is
			-- max(OutdoorAmbient, Ambient) per channel, so leaving it at black
			-- darkened nothing -- it only let Ambient's hue leak outdoors.
			Lighting.OutdoorAmbient = amb
			Lighting.FogEnd = nvgOn and Cfg.ATMO.FOG_END * 2.2
				or (powered and Cfg.ATMO.FOG_END_POWERED or Cfg.ATMO.FOG_END)
		end

		----------------------------------------------------------------------
		-- AUTO-GATING. Point a real tube at a lamp and it protects itself.
		-- Sampled at 12 Hz; the spring in the fast loop does the smoothing,
		-- so the attack still lands in about 50 ms.
		----------------------------------------------------------------------
		if nvgOn then
			local cam = workspace.CurrentCamera
			local worst = ARENA and NV.GATE_DAYLIGHT or 0
			if char and char:GetAttribute("TorchOn") == true then
				worst = math.max(worst, NV.GATE_TORCH)
			end
			if cam then
				local eye = cam.CFrame.Position
				local look = cam.CFrame.LookVector
				for _, e in allLights do
					if e.light.Enabled then
						local to = e.part.Position - eye
						local d = to.Magnitude
						if d < NV.GATE_RANGE and d > 0.1 then
							-- In front of you and close: both matter, because a
							-- tube gates on what is actually in its field.
							local facing = math.max(0, to.Unit:Dot(look))
							local near = 1 - d / NV.GATE_RANGE
							worst = math.max(worst, facing * near)
						end
					end
				end
			end
			gateTarget = math.clamp(worst, 0, 1)
		else
			gateTarget = 0
		end
		applyExposure(powered)

		----------------------------------------------------------------------
		-- Objective line
		----------------------------------------------------------------------
		local total = gameState:GetAttribute("CellsTotal") or 0
		local seated = gameState:GetAttribute("CellsInserted") or 0
		local carried = player:GetAttribute("Cells") or 0
		local text
		if ARENA then
			text = "ARENA  ///  free-for-all  --  friendly fire is on"
		elseif powered then
			text = "OBJECTIVE  ///  reach the lift"
		elseif carried > 0 then
			text = ("OBJECTIVE  ///  %d/%d cells seated  --  carrying %d, find the plant")
				:format(seated, total, carried)
		else
			text = ("OBJECTIVE  ///  %d/%d cells seated  --  find the power cells")
				:format(seated, total)
		end
		if (player:GetAttribute("Bleeding")) then
			text ..= "   [BLEEDING -- H]"
		end
		Hud.objective(text)
	end
end)

--------------------------------------------------------------------------------
-- Fast loop: dread, pain
--------------------------------------------------------------------------------
local scintAcc = 0

RunService.RenderStepped:Connect(function(dt)
	----------------------------------------------------------------------------
	-- Gate, then scintillation.
	--
	-- The gate is an AGC, not a spring: fast attack, slow release, which is why
	-- the two rates are separate. Walk into a lit corridor with the tubes down
	-- and the image slams shut and then crawls back open.
	--
	-- Scintillation is SPATIAL. Re-rolled at a fixed rate rather than every
	-- frame so it looks the same at 30 and 144 fps, and worst when the tubes
	-- are starved -- gain noise is loudest when there is least signal, which
	-- is exactly when you are straining to see something.
	----------------------------------------------------------------------------
	if nvgOn then
		local rate = gateTarget > gate and NV.GATE_ATTACK or NV.GATE_RELEASE
		gate += (gateTarget - gate) * math.clamp(dt * rate, 0, 1)
		applyExposure(gameState:GetAttribute("Powered") == true)

		scintAcc += dt * NV.SPARKLE_RATE
		if scintAcc >= 1 then
			scintAcc %= 1
			-- Sparkle harder when the tubes are starved: gain noise is loudest
			-- when there is least signal, which is exactly when you are
			-- straining to see something.
			local amp = NV.SPARKLE_OPACITY * (1 - gate * 0.6)
			for _, dot in sparkles do
				dot.Position = UDim2.fromScale(math.random(), math.random())
				dot.BackgroundTransparency = 1 - math.random() * amp
			end
		end
	end

	local char = player.Character
	local hum = char and char:FindFirstChildOfClass("Humanoid")
	local root = char and char.PrimaryPart

	----------------------------------------------------------------------------
	-- How close is it, and can it see me
	----------------------------------------------------------------------------
	local target = 0
	local entity = workspace:FindFirstChild(Cfg.ENTITY.NAME)
	if entity and root and hum and hum.Health > 0 then
		local ehrp = (entity :: Model).PrimaryPart
		if ehrp then
			local d = (ehrp.Position - root.Position).Magnitude
			local params = RaycastParams.new()
			params.FilterType = Enum.RaycastFilterType.Exclude
			params.FilterDescendantsInstances = { char :: Instance, entity }
			local blocked = workspace:Raycast(root.Position, ehrp.Position - root.Position, params) ~= nil

			local range = blocked and Cfg.ATMO.FEAR_RANGE or Cfg.ATMO.FEAR_RANGE_LOS
			if d < range then
				target = 1 - (d / range)
				if not blocked then
					target = math.min(1, target * 1.35)
				end
			end
		end
	end

	local rate = target > dread and Cfg.ATMO.FEAR_ATTACK or Cfg.ATMO.FEAR_DECAY
	dread += (target - dread) * math.clamp(dt * rate, 0, 1)

	-- The dread vignette is reused as the effects vignette -- it is already a
	-- radial darkening of the frame edges, which is exactly what suppression and
	-- being hit need, and closing the frame is the strongest cue available.
	local F0 = Cfg.SCREENFX
	Hud.setDread(math.max(
		dread,
		Fx.fire * Fx.fire * F0.FIRE_VIGNETTE,
		Fx.suppress * Fx.suppress * F0.SUPPRESS_VIGNETTE,
		Fx.hit * Fx.hit * F0.HIT_VIGNETTE
	))
	heartbeat.Volume = Cfg.SFX.HEARTBEAT.vol * dread * dread
	heartbeat.PlaybackSpeed = Cfg.SFX.HEARTBEAT.pitch * (1 + dread * 0.55)

	----------------------------------------------------------------------------
	-- Pain and screen effects.
	--
	-- The three pulses stack onto the standing pain state rather than replacing
	-- it, so being shot at while already hurt is worse than either alone -- and
	-- since there is no health bar in this game, this IS the health readout.
	----------------------------------------------------------------------------
	Fx.decay(dt)
	local F = Cfg.SCREENFX
	local baseSat = ARENA and DAY.GRADE_SATURATION or GFX.GRADE_SATURATION

	local hurt = 0
	if hum and hum.MaxHealth > 0 then
		hurt = 1 - hum.Health / hum.MaxHealth
		breathing.Volume = Cfg.SFX.BREATH.vol * math.max(hurt * 1.2, dread * 0.5)
		breathing.PlaybackSpeed = Cfg.SFX.BREATH.pitch * (1 + hurt * 0.5)
	else
		breathing.Volume = 0
	end

	-- Squared so light pulses are subtle and heavy ones are not.
	local fire2 = Fx.fire * Fx.fire
	local sup2 = Fx.suppress * Fx.suppress
	local hit2 = Fx.hit * Fx.hit

	blur.Size = hurt * hurt * 14
		+ fire2 * F.FIRE_BLUR
		+ sup2 * F.SUPPRESS_BLUR
		+ hit2 * F.HIT_BLUR

	cc.Saturation = baseSat
		- hurt * 0.5
		- sup2 * F.SUPPRESS_DESAT
		- hit2 * F.HIT_DESAT
	cc.Contrast = (ARENA and DAY.GRADE_CONTRAST or GFX.GRADE_CONTRAST) + sup2 * F.SUPPRESS_CONTRAST

	-- Hits tint red; firing gets only the very short warm exposure pulse seen
	-- in the supplied OPERATOR clip. Suppression remains colour-neutral.
	local baseTint = ARENA and DAY.GRADE_TINT or GFX.GRADE_TINT
	if hit2 > 0.01 then
		cc.TintColor = baseTint:Lerp(F.HIT_TINT, math.min(1, hit2 * 1.3))
	elseif fire2 > 0.001 then
		cc.TintColor = baseTint:Lerp(F.FIRE_TINT, math.min(1, fire2 * F.FIRE_TINT_STRENGTH))
	else
		cc.TintColor = baseTint
	end

	if not nvgOn then
		bloom.Intensity = GFX.BLOOM_INTENSITY + fire2 * F.FIRE_BLOOM
	end

	-- Deafness: everything positional gets quieter, your own breathing does not.
	local muffle = 1 - Fx.deafen * F.DEAFEN_MUFFLE
	pcall(function()
		SoundService.AmbientReverb = Fx.deafen > 0.5
			and Enum.ReverbType.Hangar
			or Enum.ReverbType.NoReverb
	end)
	heartbeat.Volume = heartbeat.Volume * muffle
end)

--------------------------------------------------------------------------------
-- Extraction banner
--------------------------------------------------------------------------------
gameState:GetAttributeChangedSignal("Powered"):Connect(function()
	if gameState:GetAttribute("Powered") then
		Hud.warn("MAIN BUS LIVE", 5)
	end
end)
