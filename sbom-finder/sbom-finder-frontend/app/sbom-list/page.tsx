"use client";

import React, { useEffect, useState } from "react";
import "./devices.css";
import Link from "next/link";

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

export default function DevicesPage() {
  const [devices, setDevices] = useState<Device[]>([]);

  useEffect(() => {
    fetch("http://localhost:8080/api/devices/all")
      .then((res) => res.json())
      .then((data) => setDevices(data))
      .catch((err) => console.error("Error fetching devices:", err));
  }, []);

  return (
    <div className="devices-container">
      <h2>Device List</h2>
      <table className="devices-table">
        <thead>
          <tr>
            <th>Device Name</th>
            <th>Manufacturer</th>
            <th>Category</th>
            <th>OS</th>
            <th>OS Version</th>
            <th>Kernel Version</th>
            <th>Digital Footprint</th>
          </tr>
        </thead>
        <tbody>
          {devices.map((device, index) => (
            <tr key={index}>
              <td>
                <Link
                  href={{
                    pathname: "/device-details",
                    query: { device_id: device.deviceId },
                  }}
                  className="text-blue-600 hover:underline"
                >
                  {device.name}
                </Link>
              </td>
              <td>{device.manufacturer}</td>
              <td>{device.category}</td>
              <td>{device.operatingSystem}</td>
              <td>{device.osVersion}</td>
              <td>{device.kernelVersion}</td>
              <td>{device.digitalFootprint}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
