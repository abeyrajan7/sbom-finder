"use client";

import React, { useEffect, useState } from "react";
import "./devices.css";
import Link from "next/link";
import SearchFilterBar from "../../components/SearchFilterBar";

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
    const BASE_URL = 'http://localhost:8080';
  const [devices, setDevices] = useState<Device[]>([]);

  useEffect(() => {
    fetch(`${BASE_URL}/api/devices/all`)
      .then((res) => res.json())
      .then((data) => setDevices(data))
      .catch((err) => console.error("Error fetching devices:", err));
  }, []);

  const handleSearch = async (params: {
    query?: string;
    manufacturer?: string;
    operatingSystem?: string;
  }) => {
    const { query = '', manufacturer = '', operatingSystem = '' } = params; // destructure here

    const res = await fetch(
      `${BASE_URL}/api/devices/search?query=${query}&manufacturer=${manufacturer}&operatingSystem=${operatingSystem}`
    );
    const data = await res.json();
    setDevices(data);
  };

  return (
    <div className="devices-container">
      <h2>Device List</h2>
      <SearchFilterBar onSearch={handleSearch} />

      <div className="table-scroll-wrapper">
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
            {devices.length === 0 ? (
              <tr>
                <td colSpan={7} style={{ textAlign: "center", padding: "1rem", color: "gray" }}>
                  No SBOM files found.
                </td>
              </tr>
            ) : (
              devices.map((device, index) => (
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
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
