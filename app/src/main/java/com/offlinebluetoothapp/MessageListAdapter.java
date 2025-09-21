package com.offlinebluetoothapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageListAdapter extends RecyclerView.Adapter<MessageListAdapter.MessageViewHolder> {
    
    private List<Message> messages;
    
    public MessageListAdapter(List<Message> messages) {
        this.messages = messages;
    }
    
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        
        holder.messageSender.setText(message.getSender() + ":");
        holder.messageContent.setText(message.getContent());
        holder.messageTime.setText(message.getTimestamp());
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageSender;
        TextView messageContent;
        TextView messageTime;
        
        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageSender = itemView.findViewById(R.id.message_sender);
            messageContent = itemView.findViewById(R.id.message_content);
            messageTime = itemView.findViewById(R.id.message_time);
        }
    }
}