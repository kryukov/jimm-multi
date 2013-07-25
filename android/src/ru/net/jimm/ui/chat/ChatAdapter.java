package ru.net.jimm.ui.chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import jimmui.model.chat.ChatModel;
import ru.net.jimm.R;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 21.07.13 2:54
 *
 * @author vladimir
 */
public class ChatAdapter extends BaseAdapter {
    private ChatModel chat;
    private LayoutInflater layoutInflater;
    public ChatAdapter(Context context, ChatModel chat) {
        this.chat = chat;
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return chat.size();
    }

    @Override
    public Object getItem(int i) {
        return chat.getMessage(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int index, View view, ViewGroup viewGroup) {
            ViewHolder holder;
            if (view == null) {
                view = layoutInflater.inflate(R.layout.chat_message, null);
                    holder = new ViewHolder();
                    holder.from = (TextView) view.findViewById(R.id.chat_message_from);
                    holder.date = (TextView) view.findViewById(R.id.chat_message_time);
                    holder.text = (TextView) view.findViewById(R.id.chat_message_text);
                view.setTag(holder);
                } else {
                    holder = (ViewHolder) view.getTag();
                }
     
            holder.from.setText(chat.getMessage(index).getNick());
            holder.date.setText("By, " + chat.getMessage(index).getTime());
            holder.text.setText(chat.getMessage(index).getText());
     
            return view;
    }

    private static class ViewHolder {
        TextView from;
        TextView date;
        TextView text;
    }
}
