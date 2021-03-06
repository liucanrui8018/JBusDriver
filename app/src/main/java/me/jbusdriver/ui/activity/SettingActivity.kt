package me.jbusdriver.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.Toolbar
import com.afollestad.materialdialogs.MaterialDialog
import com.chad.library.adapter.base.entity.MultiItemEntity
import jbusdriver.me.jbusdriver.R
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.layout_menu_op_item.view.*
import me.jbusdriver.common.BaseActivity
import me.jbusdriver.common.KLog
import me.jbusdriver.common.spanCount
import me.jbusdriver.mvp.bean.Expand_Type_Head
import me.jbusdriver.mvp.bean.MenuOp
import me.jbusdriver.mvp.bean.MenuOpHead
import me.jbusdriver.ui.adapter.MenuOpAdapter
import me.jbusdriver.ui.data.AppConfiguration
import me.jbusdriver.ui.data.magnet.MagnetLoaders

class SettingActivity : BaseActivity() {

    private var pageModeHolder = AppConfiguration.pageMode
    private val menuOpValue by lazy { AppConfiguration.menuConfig.toMutableMap() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        setToolBar()
        initSettingView()
    }

    private fun initSettingView() {
        //page mode
        changePageMode(AppConfiguration.pageMode)
        ll_page_mode_page.setOnClickListener {
            pageModeHolder = AppConfiguration.PageMode.Page
            changePageMode(AppConfiguration.PageMode.Page)
        }
        ll_page_mode_normal.setOnClickListener {
            pageModeHolder = AppConfiguration.PageMode.Normal
            changePageMode(AppConfiguration.PageMode.Normal)
        }

        //menu op
        val data: List<MultiItemEntity> = arrayListOf(
                MenuOpHead("个人").apply { MenuOp.mine.forEach { addSubItem(it) } },
                MenuOpHead("有碼").apply { MenuOp.nav_ma.forEach { addSubItem(it) } },
                MenuOpHead("無碼").apply { MenuOp.nav_uncensore.forEach { addSubItem(it) } },
                MenuOpHead("欧美").apply { MenuOp.nav_xyz.forEach { addSubItem(it) } },
                MenuOpHead("其他").apply { MenuOp.nav_other.forEach { addSubItem(it) } }
        )
        val adapter = MenuOpAdapter(data)
        rv_menu_op.adapter = adapter
        rv_menu_op.layoutManager = GridLayoutManager(viewContext, viewContext.spanCount).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) =
                        if (adapter.getItemViewType(position) == Expand_Type_Head) spanCount else 1
            }
        }
        //有选项选中就展开
        val expandItems = data.filterIndexed { index, multiItemEntity ->
            multiItemEntity is MenuOpHead && multiItemEntity.subItems.any { it.isHow }
        }
        expandItems.forEach {
            adapter.expand(data.indexOf(it))
        }
        adapter.setOnItemClickListener { adapter, view, position ->
            KLog.d("MenuOpAdapter : setOnItemClickListener ${data[position]}")

            (adapter.data.getOrNull(position) as? MenuOp)?.let {
                view.cb_nav_menu?.let { cb ->
                    //添加设置
                    synchronized(cb) {
                        cb.isChecked = !cb.isChecked
                        menuOpValue.put(it.name, cb.isChecked)
                        KLog.d("menuConfig ${menuOpValue.filter { it.value }}")
                    }
                }
            }

        }

        //magnet source
        tv_magnet_source.text = AppConfiguration.MagnetKeys.joinToString(separator = "   ")
        ll_magnet_source_config.setOnClickListener {

            val selectedIndices = AppConfiguration.MagnetKeys.map { MagnetLoaders.keys.indexOf(it) }.toTypedArray()

            val disables = if (selectedIndices.size <= 1) selectedIndices else emptyArray()

            MaterialDialog.Builder(viewContext).title("磁力源配置")
                    .items(MagnetLoaders.keys.toList())
                    .itemsCallbackMultiChoice(selectedIndices) { dialog, which, _ ->
                        if (which.size <= 1) {
                            dialog.builder.itemsDisabledIndices(*which)
                        } else {
                            dialog.builder.itemsDisabledIndices()
                        }
                        dialog.notifyItemsChanged()
                        return@itemsCallbackMultiChoice true
                    }.alwaysCallMultiChoiceCallback()
                    .itemsDisabledIndices(*disables)
                    .negativeText("取消")
                    .negativeColor(R.color.secondText)
                    .positiveText("配置")
                    .onPositive { dialog, which ->
                        AppConfiguration.MagnetKeys.clear()
                        AppConfiguration.MagnetKeys.addAll(dialog.selectedIndices?.mapNotNull { MagnetLoaders.keys.toList().getOrNull(it) } ?: emptyList())
                        AppConfiguration.saveMagnetKeys()
                        tv_magnet_source.text = AppConfiguration.MagnetKeys.joinToString(separator = "   ")
                    }
                    .show()
        }

    }

    private fun changePageMode(mode: Int) {
        when (mode) {
            AppConfiguration.PageMode.Page -> {
                ll_page_mode_page.setBackgroundResource(R.drawable.mode_page_shape_corner)
                ll_page_mode_normal.setBackgroundResource(0)
            }
            AppConfiguration.PageMode.Normal -> {
                ll_page_mode_page.setBackgroundResource(0)
                ll_page_mode_normal.setBackgroundResource(R.drawable.mode_page_shape_corner)
            }
        }
    }

    private fun setToolBar() {
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = "设置"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onStop() {
        super.onStop()
        AppConfiguration.pageMode = pageModeHolder
        if (!AppConfiguration.menuConfig.equals(menuOpValue)) AppConfiguration.saveSaveMenuConfig(menuOpValue) //必须调用equals
    }

    companion object {
        fun start(context: Context) = context.startActivity(Intent(context, SettingActivity::class.java))
    }
}
