# Seeds the emulator with a small, plausible demo library so the goldie
# screenshots show a real app rather than empty states.
#
# The files land in /sdcard/Music/Abstra Demo, which is inside the Music
# collection, so MediaStore indexes them with IS_MUSIC = 1 and Abstra Pro's
# library scan (which filters on IS_MUSIC != 0) picks them up.
#
# Every track gets a matching "<name>.lrc" next to it, which is exactly where
# MusicViewModel looks for synced lyrics.
#
#   pwsh -File goldie/seed-library.ps1

$ErrorActionPreference = "Stop"
$Sdk = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { "$env:LOCALAPPDATA\Android\Sdk" }
$Adb = "$Sdk\platform-tools\adb.exe"

$Device = if ($env:GOLDIE_EMULATOR_SERIAL) { $env:GOLDIE_EMULATOR_SERIAL } else { "emulator-5554" }
$Remote = "/sdcard/Music/Abstra Demo"
$Stage = Join-Path $env:TEMP "abstra-demo-library"

if (-not (Get-Command ffmpeg -ErrorAction SilentlyContinue)) {
    throw "ffmpeg not found on PATH. Install it (winget install Gyan.FFmpeg) and reopen the shell."
}

# title, artist, album, duration, genre, base frequency (Hz)
$Tracks = @(
    @{ t = "Midnight Polaroid"; a = "Kaya Vance";   al = "Neon Static";    d = 238; g = "Pop";      f = 220 }
    @{ t = "Slow Bloom";        a = "Kaya Vance";   al = "Neon Static";    d = 196; g = "Pop";      f = 262 }
    @{ t = "Static Lullaby";    a = "Kaya Vance";   al = "Neon Static";    d = 274; g = "Pop";      f = 175 }
    @{ t = "Paper Lanterns";    a = "The Hollow Hours"; al = "Paper Lanterns"; d = 212; g = "Indie"; f = 294 }
    @{ t = "Copper Rain";       a = "The Hollow Hours"; al = "Paper Lanterns"; d = 251; g = "Indie"; f = 208 }
    @{ t = "Glasshouse";        a = "Mira Sol";     al = "Field Notes";     d = 187; g = "Ambient";  f = 330 }
    @{ t = "Night Bus";         a = "Mira Sol";     al = "Field Notes";     d = 229; g = "Ambient";  f = 247 }
    @{ t = "Low Tide";          a = "Atlas Grey";   al = "Field Notes";     d = 265; g = "Electronic"; f = 196 }
    @{ t = "Sunroom";           a = "Atlas Grey";   al = "Long Way Down";   d = 203; g = "Electronic"; f = 277 }
    @{ t = "Amber Signal";      a = "Ada Lune";     al = "Long Way Down";   d = 244; g = "Electronic"; f = 233 }
    @{ t = "Tidal Drift";       a = "Ada Lune";     al = "Long Way Down";   d = 289; g = "Electronic"; f = 165 }
    @{ t = "Winter Ferry";      a = "Ada Lune";     al = "Long Way Down";   d = 221; g = "Electronic"; f = 311 }
)

function New-Lrc {
    param([string]$Title, [string]$Artist, [int]$Seconds)

    $lines = @(
        "City lights dissolve in the glass",
        "Every window keeping a different past",
        "I count the hours by the river flow",
        "And let the quiet take the wheel and go",
        "Hold the moment, let it turn",
        "Nothing here is what it was",
        "Somewhere between the breath and beat",
        "We are only what we choose to keep",
        "So play it out, play it slow",
        "Let the last of the daylight go",
        "And when the morning finds the room",
        "I will still be here, still in tune"
    )

    $sb = [System.Text.StringBuilder]::new()
    [void]$sb.AppendLine("[ti:$Title]")
    [void]$sb.AppendLine("[ar:$Artist]")
    [void]$sb.AppendLine("[al:Field Notes]")
    [void]$sb.AppendLine("[length:0$Seconds]")
    [void]$sb.AppendLine("[by:Abstra Pro demo library]")

    # Spread the lines over the first ~70% of the track so there is still
    # synced lyric content on screen when playback is only a few seconds in.
    $span = [int]($Seconds * 0.7)
    $step = [int]($span / $lines.Count)
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $t = 6 + ($i * $step)
        $mm = [int]($t / 60)
        $ss = $t % 60
        $ms = ($i * 137) % 1000
        [void]$sb.AppendLine(("[{0:d2}:{1:d2}.{2:d3}]{3}" -f $mm, $ss, $ms, $lines[$i]))
    }
    return $sb.ToString()
}

Write-Host "Staging demo library in $Stage"
if (Test-Path $Stage) { Remove-Item $Stage -Recurse -Force }
New-Item -ItemType Directory -Path $Stage | Out-Null

$year = 2024
foreach ($tr in $Tracks) {
    $mp3 = Join-Path $Stage "$($tr.t).mp3"
    # A soft two-note chord so the waveform visualizer and the visualiser in the
    # full player have something to react to.
    $fifth = [int]($tr.f * 1.5)
    & ffmpeg -y -loglevel error `
        -f lavfi -i "sine=frequency=$($tr.f):duration=$($tr.d):sample_rate=44100" `
        -f lavfi -i "sine=frequency=${fifth}:duration=$($tr.d):sample_rate=44100" `
        -filter_complex "[0:a]volume=0.55[a0];[1:a]volume=0.30,tremolo=f=0.4:d=0.7[a1];[a0][a1]amix=inputs=2:duration=first,afade=t=in:d=2,afade=t=out:st=$($tr.d - 3):d=3,aformat=channel_layouts=stereo[out]" `
        -map "[out]" -c:a libmp3lame -b:a 96k `
        -metadata "title=$($tr.t)" `
        -metadata "artist=$($tr.a)" `
        -metadata "album=$($tr.al)" `
        -metadata "album_artist=$($tr.a)" `
        -metadata "genre=$($tr.g)" `
        -metadata "date=$year" `
        -metadata "track=${year}" `
        -id3v2_version 3 -write_id3v1 1 `
        $mp3
    if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed for $($tr.t)" }

    New-Lrc -Title $tr.t -Artist $tr.a -Seconds $tr.d | Set-Content -Path (Join-Path $Stage "$($tr.t).lrc") -Encoding utf8
    $year++
    Write-Host "  built $($tr.t) ($($tr.d)s)"
}

Write-Host "Pushing to ${Device}:$Remote"
& $Adb -s $Device shell "mkdir -p '$Remote'"
& $Adb -s $Device push "$Stage\." "$Remote/"

Write-Host "Waiting for MediaStore to index the tracks"
Start-Sleep -Seconds 5

# adb push already registers the new files with MediaStore on a running device.
# Abstra Pro's own scan filters on is_music != 0, so that is what we assert here.
$rows = & $Adb -s $Device shell "content query --uri content://media/external/audio/media --projection title:artist:album:is_music:duration"
$indexed = @($rows | Where-Object { $_ -match "is_music=1" })
Write-Host ""
Write-Host "Indexed tracks ($($indexed.Count) of $($Tracks.Count) with is_music=1):"
$indexed
if ($indexed.Count -lt $Tracks.Count) {
    throw "Only $($indexed.Count) of $($Tracks.Count) tracks were indexed as music. Abstra Pro's scan filters on is_music != 0 and would show an empty library."
}
