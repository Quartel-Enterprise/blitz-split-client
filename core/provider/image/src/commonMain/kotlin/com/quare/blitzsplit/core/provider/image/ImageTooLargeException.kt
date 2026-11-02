package com.quare.blitzsplit.core.provider.image

// Why: a readable photo so full of detail that not even the lowest quality brings it under the bill image limit.
class ImageTooLargeException : Exception("The picked image stays over the size limit at every quality")
