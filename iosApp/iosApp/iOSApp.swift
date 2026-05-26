import SwiftUI
import ComposeApp

private let appGroup = "group.com.mettyoung.deconstructchinese"
private let sharedTextKey = "shared_text"

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in handleIncomingURL(url) }
        }
    }

    private func handleIncomingURL(_ url: URL) {
        guard url.scheme == "deconstructchinese" else { return }
        let defaults = UserDefaults(suiteName: appGroup)
        if let text = defaults?.string(forKey: sharedTextKey), !text.isEmpty {
            // Bus is CONFLATED, so this is buffered until Compose starts collecting.
            IncomingTextKt.submitSharedText(text: text)
            defaults?.removeObject(forKey: sharedTextKey)
        }
    }
}
