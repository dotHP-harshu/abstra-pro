const APP_ROOT = "D:/abstra-pro";
const APK = `${APP_ROOT}/app/build/outputs/apk/release/app-release.apk`;

const config = {
  appRoot: APP_ROOT,
  flowsDir: "../.argent/flows",

  // Abstra Pro is a native Android app with no iOS target, so the Play phone
  // is the only device. appPath/bundleId stay pointed at the same release
  // build; goldie never reads them for an android device key.
  appPath: APK,
  bundleId: "dev.abstra.pro",
  android: {
    appPath: APK,
    applicationId: "dev.abstra.pro",
  },

  devices: ["pixel-10-pro"],
  locales: ["en-US"],
  appearance: "light",

  theme: {
    // Soft cotton-candy gradient picked from the app's default COTTON_CANDY palette.
    background: "linear-gradient(160deg, #FFD9E6 0%, #FFEFF5 48%, #EFE4FF 100%)",
    headlineColor: "#2B0F22",
    subheadColor: "#6B4A5E",
    fontFamily: "Montserrat",
    copyHeightRatio: 0.24,
    deviceWidthRatio: 0.84,
    layout: "classic",
  },

  // Android tiles are framed with goldie's bundled Pixel 10 Pro bezel, so the
  // iPhone variants below are unused. Kept so the config stays a valid
  // GoldieConfig.
  frame: { variant: "17-pro-blue" },

  store: {
    name: "Abstra Pro",
    subtitle: { "en-US": "Offline Music Player" },
    developer: "Abstra",
    category: "Music & Audio",
    rating: 4.8,
    ratingCount: "1.2K Ratings",
    ageRating: "Everyone",
    price: "Free",
    description: {
      "en-US":
        "Abstra Pro is a complete offline music player for the audio already on your phone. It ships with zero internet permission, so nothing about your listening ever leaves the device.\n\nScan your library, build playlists, and let on-device smart mixes find something worth playing. A full player with synced lyrics, a real multi-band DSP chain, a sleep timer and gapless playback does the rest.\n\nSeven themes, three player centerpieces, and background playback that keeps going when you close the app.",
    },
  },

  scenes: [
    {
      kind: "screenshot",
      id: "home",
      flow: "store-01-home",
      layout: "hero",
      headline: { "en-US": "Zero internet. All your music." },
      subhead: { "en-US": "Abstra Pro never touches the network — just the audio already on your phone." },
      decorations: [
        {
          kind: "badge",
          text: { "en-US": "100% offline" },
          position: "bottom-left",
          background: "#2B0F22",
          color: "#FFE3EC",
        },
      ],
    },
    {
      kind: "screenshot",
      id: "player",
      flow: "store-02-player",
      layout: "classic",
      headline: { "en-US": "A player that feels good" },
      subhead: { "en-US": "Vinyl, CD or Polaroid, with a visualizer that moves with every track." },
    },
    {
      kind: "screenshot",
      id: "dsp",
      flow: "store-03-dsp",
      layout: "offset",
      headline: { "en-US": "Shape the sound" },
      subhead: { "en-US": "Multi-band EQ, bass boost, reverb and a real limiter — all on device." },
    },
    {
      kind: "screenshot",
      id: "library",
      flow: "store-04-library",
      layout: "copy-below",
      headline: { "en-US": "Find any track in seconds" },
      subhead: { "en-US": "Search, sort and filter your whole library, with smart playlists." },
    },
    {
      kind: "screenshot",
      id: "lyrics",
      flow: "store-05-lyrics",
      layout: "tilt",
      headline: { "en-US": "Synced lyrics, word by word" },
      subhead: { "en-US": "Drop in a .LRC file and nudge the timing until it lands perfectly." },
    },

    {
      kind: "preview",
      id: "preview",
      segments: [
        { id: "home", flow: "store-preview-01-home" },
        { id: "browse", flow: "store-preview-02-browse" },
        { id: "play", flow: "store-preview-03-play", holdSeconds: 2 },
      ],
    },
  ],
};

export default config;
