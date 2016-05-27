package com.syro.testbluetooth10.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.syro.testbluetooth10.Activity.UnpairDvcActivity;
import com.syro.testbluetooth10.R;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Syro on 2015-12-24.
 */
public class DeviceListAdapter extends BaseAdapter {
    private List<HashMap<String, Object>> list;
    private int layoutRes;
    private LayoutInflater layoutInflater;
    private Context context;

    public DeviceListAdapter(Context context, List<HashMap<String, Object>> list, int layoutRes) {
        this.context = context;
        this.list = list;//整个列表的数据
        this.layoutRes = layoutRes;//单个条目的资源文件
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) { //第一次创建的item
            convertView = layoutInflater.inflate(layoutRes, null);
            viewHolder = new ViewHolder();
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.dvc_img);
            viewHolder.dvcName = (TextView) convertView.findViewById(R.id.dvc_name);
            viewHolder.dvcAddr = (TextView) convertView.findViewById(R.id.dvc_addr);
            viewHolder.goToUnpair = (Button) convertView.findViewById(R.id.dvc_unpair);
            convertView.setTag(viewHolder);//store data within view
        } else { //缓存中已有的item
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final HashMap map = list.get(position);//获取一个item所需的数据
        viewHolder.goToUnpair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, UnpairDvcActivity.class);
                intent.putExtra("pairDvcName", map.get("name").toString());
                intent.putExtra("pairDvcAddr", map.get("addr").toString());
                context.startActivity(intent);
            }
        });
        viewHolder.imageView.setImageResource((Integer) map.get("img"));
        viewHolder.dvcName.setText(map.get("name").toString());
        viewHolder.dvcAddr.setText(map.get("addr").toString());
        viewHolder.goToUnpair.setText(">");

        return convertView;
    }

    private static class ViewHolder {
        ImageView imageView;
        TextView dvcName;
        TextView dvcAddr;
        Button goToUnpair;
    }
}
