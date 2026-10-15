# Dumps the current emulator screen as readable text/content-desc lines, so flow
# selectors can be checked against the real UI instead of guessed.
param([string]$Serial = "emulator-5554")

# Reads the hierarchy straight off stdout: /sdcard is not readable from adb shell
# on this image once the app package has been replaced.
$xml = adb -s $Serial exec-out uiautomator dump /dev/tty 2>$null
$xml = ($xml -join "") -replace 'UI hierchary dumped.*$', ''

[xml]$doc = $xml
foreach ($n in $doc.SelectNodes("//node")) {
    $text = $n.GetAttribute("text")
    $desc = $n.GetAttribute("content-desc")
    if (-not $text -and -not $desc) { continue }
    $label = if ($text) { $text } else { $desc }
    $bounds = $n.GetAttribute("bounds")
    if ($bounds -match '\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]') {
        $x = ([int]$Matches[1] + [int]$Matches[3]) / 2
        $y = ([int]$Matches[2] + [int]$Matches[4]) / 2
        $where = "x={0:N3} y={1:N3}" -f ($x / 1080), ($y / 2410)
    } else { $where = "?" }
    $kind = if ($text -and $desc) { "text+desc" } elseif ($text) { "text" } else { "desc" }
    "{0,-9} {1,-42} {2}" -f $kind, $label, $where
}
