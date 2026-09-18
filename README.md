# Shqip TV — Android TV IPTV

A remote-first Android TV / Google TV IPTV player with a dark blue TV interface, fast D-pad navigation, M3U/M3U8 playlist support, Xtream Codes live-channel support, and full-screen Media3/ExoPlayer playback.

## What works in this build

- Android TV / Google TV launcher support
- M3U / M3U8 URL import
- Xtream Codes server + username + password login
- Live channel categories
- Search
- Channel logos
- Fast two-column RecyclerView optimized for D-pad navigation
- OK/Select on any channel opens full-screen playback immediately
- Back returns to the channel grid
- Cached channel list if the provider is temporarily unreachable
- GitHub Actions APK build

Movies, Series, Favorites, and XMLTV EPG are visible in the design and are the next feature modules; the first build focuses on making Live TV fast and reliable.

## Build the APK using GitHub

1. Create a new empty GitHub repository.
2. Upload the contents of this project to the repository root.
3. Commit the files.
4. Open **Actions** → **Build Android TV APK**.
5. Choose **Run workflow**.
6. When the workflow finishes, open the run and download the artifact named **ShqipTV-debug-apk**.
7. Extract the artifact ZIP. Inside is `app-debug.apk`.
8. Install that APK on your Android TV / Google TV device.

## First launch

Choose either **M3U / M3U8 URL** or **Xtream Codes**, enter credentials for a service/playlist you are authorized to use, then choose **Save & Load Channels**.

## Remote behavior

- D-pad arrows: navigate
- OK / Select on a channel: open full-screen playback
- Back: return to the same channel list screen
- OK during playback: show/hide player controls

## Security note

This sample stores source settings locally using Android SharedPreferences for convenience. For a production release, move credentials to encrypted storage and require HTTPS where your provider supports it.
