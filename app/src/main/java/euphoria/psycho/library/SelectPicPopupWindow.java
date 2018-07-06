package euphoria.psycho.library;


import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SelectPicPopupWindow extends PopupWindow {


    private List<Map<String, Object>> data_list;
    // 图片封装为一个数组
    private int[] icon = {R.mipmap.controlbar_menu1, R.mipmap.controlbar_menu1};
    private String[] iconName = {"001", "002"};
    private GridView image_gv;
    private View mMenuView;
    private SimpleAdapter sim_adapter;

    /**
     * 获取组装数据map
     *
     * @return
     */
    public List<Map<String, Object>> getData() {
//cion和iconName的长度是相同的，这里任选其一都可以
        for (int i = 0; i < icon.length; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("image", icon[i]);
            map.put("text", iconName[i]);
            data_list.add(map);
        }

        return data_list;
    }

    /**
     * 返回图标数组
     *
     * @return
     */
    public int[] getIcon() {
        return icon;
    }

    public SelectPicPopupWindow(Activity context, AdapterView.OnItemClickListener itemsOnClick) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//利用布局生成view
        mMenuView = inflater.inflate(R.layout.popwindow_choose_icon, null);
        image_gv = (GridView) mMenuView.findViewById(R.id.image_gv);
//新建List
        data_list = new ArrayList<Map<String, Object>>();
//获取数据
        getData();
//新建适配器
        String[] from = {"image", "text"};
        int[] to = {R.id.image, R.id.text};
        sim_adapter = new SimpleAdapter(context, data_list, R.layout.item_pic_gv, from, to);
//配置适配器
        image_gv.setAdapter(sim_adapter);

//设置按钮监听
        image_gv.setOnItemClickListener(itemsOnClick);
//设置SelectPicPopupWindow的View
        this.setContentView(mMenuView);
//设置SelectPicPopupWindow弹出窗体的宽
        this.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
//设置SelectPicPopupWindow弹出窗体的高
        this.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
//设置SelectPicPopupWindow弹出窗体可点击
        this.setFocusable(true);
//设置SelectPicPopupWindow弹出窗体动画效果
        this.setAnimationStyle(R.style.mypopwindow_anim_style);
//实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0xb0000000);
//设置SelectPicPopupWindow弹出窗体的背景
        this.setBackgroundDrawable(dw);
//mMenuView添加OnTouchListener监听判断获取触屏位置如果在选择框外面则销毁弹出框
        mMenuView.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {

                int height = mMenuView.findViewById(R.id.pop_layout).getTop();
                int y = (int) event.getY();
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (y < height) {
                        dismiss();
                    }
                }
                return true;
            }
        });

    }
}