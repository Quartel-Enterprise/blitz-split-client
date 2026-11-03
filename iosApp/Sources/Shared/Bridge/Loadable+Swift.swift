import Shared

/// Reads a Kotlin `Loadable<T>` (`software.quare:loadable`), whose generic doesn't survive the Objective-C header:
/// Swift sees the `Loadable` protocol with `LoadableLoading` and `LoadableLoaded` (a value typed `Any?`).
///
/// ```swift
/// if let people = state.people.loaded(as: [HomePerson].self) { … } else { /* shimmer */ }
/// ```
extension Loadable {
    /// True until the section has an answer: the screen draws a shimmer there, never an empty state or a zero.
    var isLoading: Bool {
        self is LoadableLoading
    }

    /// The value once loaded, cast to what the Kotlin side holds (a `List` is a Swift array, an `Int` a `KotlinInt`
    /// or `Int`); nil while loading. A `Loaded(null)` is nil too: tell it apart with `isLoading`.
    func loaded<T>(as type: T.Type) -> T? {
        (self as? LoadableLoaded<AnyObject>)?.value as? T
    }
}
