package dev.ragnarok.fenrir.mvp.compat

import android.os.Bundle
import android.view.View
import androidx.loader.app.LoaderManager
import dev.ragnarok.fenrir.mvp.core.IMvpView
import dev.ragnarok.fenrir.mvp.core.IPresenter
import dev.ragnarok.fenrir.mvp.core.PresenterAction
import dev.ragnarok.fenrir.mvp.core.RetPresenterAction

abstract class AbsMvpDialogFragment<P : IPresenter<V>, V : IMvpView> :
    androidx.fragment.app.DialogFragment(), ViewHostDelegate.IFactoryProvider<P, V> {

    private val delegate = ViewHostDelegate<P, V>()

    protected val presenter: P?
        get() = delegate.presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        delegate.onCreate(
            requireActivity(),
            getViewHost(),
            this,
            LoaderManager.getInstance(this),
            savedInstanceState
        )
    }

    // Override in case of fragment not implementing IPresenter<View> interface
    @Suppress("UNCHECKED_CAST")
    @SuppressWarnings("unchecked")
    private fun getViewHost(): V = this as V

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        delegate.onViewCreated()
    }

    protected fun fireViewCreated() {
        delegate.onViewCreated()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        delegate.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        delegate.onPause()
    }

    override fun onResume() {
        super.onResume()
        delegate.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        delegate.onDestroyView()
    }

    fun callPresenter(action: PresenterAction<P, V>) {
        delegate.callPresenter(action)
    }

    fun <T> callPresenter(action: RetPresenterAction<P, V, T>, onDefault: T): T {
        return delegate.callPresenter(action, onDefault)
    }

    fun postPrenseterReceive(action: PresenterAction<P, V>) {
        delegate.postPrenseterReceive(action)
    }

    override fun onDestroy() {
        delegate.onDestroy()
        super.onDestroy()
    }

}