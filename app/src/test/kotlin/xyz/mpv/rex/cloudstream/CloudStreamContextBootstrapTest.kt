package xyz.mpv.rex.cloudstream

import android.content.Context
import android.content.ContextWrapper
import com.lagradost.api.getContext
import com.lagradost.api.setContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class CloudStreamContextBootstrapTest {

  @Test
  fun testContextBootstrapLifecycle() {
    // 1. Initial or reset state
    setContext(null)
    assertNull("Context should be null after setting null", getContext())

    // 2. Setting a non-Context object should result in null due to safe cast (context as? Context)
    setContext("not a context")
    assertNull("Context should be null when a non-Context is passed", getContext())

    // 3. Setting a valid Context
    val dummyContext: Context = ContextWrapper(null)
    setContext(dummyContext)
    assertSame("getContext should return the registered context instance", dummyContext, getContext())

    // 4. Verification that setContext is idempotent and safe for repeated calls
    val secondContext: Context = ContextWrapper(null)
    setContext(secondContext)
    assertSame("getContext should reflect updated context", secondContext, getContext())

    // 5. Cleanup
    setContext(null)
    assertNull(getContext())
  }
}
