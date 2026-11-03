package com.quare.blitzsplit.feature.billdetail

import software.quare.loadable.Loadable

// Fails the test when the section is still loading, so an assertion never passes on a list that isn't there.
internal fun <T> Loadable<List<T>>.loadedItems(): List<T> = (this as Loadable.Loaded).value
