package com.offlinebluetoothapp;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {
    
    private List<BluetoothDevice> devices;
    private OnDeviceClickListener listener;
    
    public interface OnDeviceClickListener {
        void onDeviceClick(BluetoothDevice device);
    }
    
    public DeviceListAdapter(List<BluetoothDevice> devices, OnDeviceClickListener listener) {
        this.devices = devices;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        BluetoothDevice device = devices.get(position);
        
        String deviceName = "Unknown Device";
        if (ActivityCompat.checkSelfPermission(holder.itemView.getContext(), 
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            deviceName = device.getName();
        }
        if (deviceName == null) deviceName = "Unknown Device";
        
        holder.deviceName.setText(deviceName);
        holder.deviceAddress.setText(device.getAddress());
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeviceClick(device);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return devices.size();
    }
    
    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        
        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_name);
            deviceAddress = itemView.findViewById(R.id.device_address);
        }
    }
}