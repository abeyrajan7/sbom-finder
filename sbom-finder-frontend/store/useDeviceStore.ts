import { create } from 'zustand';

interface Device {
  name: string;
  deviceId: number;
  manufacturer: string;
  category: string;
  operatingSystem: string;
  osVersion: string;
  kernelVersion: string;
  digitalFootprint: string;
  sbomId: number;
}

interface DeviceStore {
  devices: Device[];
  setDevices: (devices: Device[]) => void;
  fetchDevices: () => Promise<void>;
}

export const useDeviceStore = create<DeviceStore>((set) => ({
  devices: [],
  setDevices: (devices) => set({ devices }),
  fetchDevices: async () => {
    const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
    const res = await fetch(`${BASE_URL}/api/devices/all`);
    const data = await res.json();
    const sorted = data.sort((a: Device, b: Device) => b.sbomId - a.sbomId);
    set({ devices: sorted });
  },
}));
