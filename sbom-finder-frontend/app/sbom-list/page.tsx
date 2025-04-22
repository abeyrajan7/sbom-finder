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
  const BASE_URL = 'https://sbom-finder-backend.onrender.com';
//   const BASE_URL = "http://localhost:8080";
  const [devices, setDevices] = useState<Device[]>([]);
  const [openDropdown, setOpenDropdown] = useState<number | null>(null);

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

  const fetchDevices = async () => {
    try {
      const res = await fetch(`${BASE_URL}/api/devices/all`);
      const data = await res.json();
      setDevices(data);
    } catch (err) {
      console.error("Error fetching devices:", err);
    }
  };

  const handleDelete = async (deviceId : number) => {
    if (confirm("Are you sure you want to delete this SBOM?")) {
      try {
        const response = await fetch(`${BASE_URL}/api/sboms/${deviceId}`, {
          method: "DELETE",
        });

        if (response.ok) {
          alert("SBOM deleted successfully!");
          // Optionally, refresh device list after deletion
          fetchDevices();
        } else {
          alert("Failed to delete SBOM.");
        }
      } catch (error) {
        console.error("Error deleting device:", error);
        alert("An error occurred while deleting.");
      }
    }
  };

  const toggleDropdown = (index: number) => {
    setOpenDropdown((prev) => (prev === index ? null : index));
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
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {devices.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ textAlign: "center", padding: "1rem", color: "gray" }}>
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
                  <td>
                    <div className="relative flex items-center space-x-4">
                      {/* Delete Button */}
                      <button
                        className="text-red-500 hover:underline"
                        onClick={() => handleDelete(device.deviceId)}
                      >
                        Delete
                      </button>

                      {/* Download Button */}
                      <button
                        onClick={() => toggleDropdown(index)}
                        className="text-blue-500 hover:text-blue-700"
                        title="Download SBOM"
                      >
                        Download SBOM
                      </button>

                      {/* Dropdown menu */}
                      {openDropdown === index && (
                        <div className="absolute right-0 mt-2 w-40 bg-white border border-gray-300 rounded shadow-md z-10">
                          <a
                            href={`${BASE_URL}/api/devices/download/${device.deviceId}?format=cyclonedx`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="block px-4 py-2 text-gray-700 hover:bg-gray-100"
                          >
                            CycloneDX
                          </a>
                          <a
                            href={`${BASE_URL}/api/devices/download/${device.deviceId}?format=spdx`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="block px-4 py-2 text-gray-700 hover:bg-gray-100"
                          >
                            SPDX
                          </a>
                        </div>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
