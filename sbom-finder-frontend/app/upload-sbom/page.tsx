"use client";

import React, { useState, useRef } from "react";
import "./upload.css";

export default function UploadSBOMPage() {
//   const BASE_URL = "http://localhost:8080";
  const BASE_URL = 'https://sbom-finder-backend.onrender.com';
  const [uploadType, setUploadType] = useState<"archive" | "dependency">("archive");
  const [file, setFile] = useState<File | null>(null);
  const [manufacturer, setManufacturer] = useState("");
  const [category, setCategory] = useState("");
  const [operatingSystem, setOperatingSystem] = useState("");
  const [osVersion, setOsVersion] = useState("");
  const [kernelVersion, setKernelVersion] = useState("");
  const [deviceName, setDeviceName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleUpload = async () => {
    setError(null);

    if (!file) {
      setError("Please select a file to upload.");
      return;
    }

    if (!category || !deviceName) {
      setError("Please fill all required fields.");
      return;
    }

    setLoading(true);

    const formData = new FormData();
    formData.append("file", file);
    formData.append("category", category);
    formData.append("deviceName", deviceName);
    formData.append("manufacturer", manufacturer || "Unknown Manufacturer");
    formData.append("operatingSystem", operatingSystem || "Unknown OS");
    formData.append("osVersion", osVersion || "Unknown Version");
    formData.append("kernelVersion", kernelVersion || "Unknown Kernel");

    try {
      let response;
      if (uploadType === "archive") {
        response = await fetch(`${BASE_URL}/api/sboms/upload-source`, {
          method: "POST",
          body: formData,
        });
      } else {
        response = await fetch(`${BASE_URL}/api/sboms/upload-dependency`, {
          method: "POST",
          body: formData,
        });
      }

      if (response.ok) {
        const result = await response.text();
        alert(`Success: ${result}`);
        resetForm();
      } else {
        const result = await response.text();
        setError(result);
      }
    } catch (err) {
      console.error("Upload Error:", err);
      setError("Upload failed.");
    } finally {
      setLoading(false);
    }
  };

  const resetForm = () => {
    setFile(null);
    setManufacturer("");
    setOperatingSystem("");
    setOsVersion("");
    setKernelVersion("");
    setDeviceName("");
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  return (
    <div className="upload-container">
      <div className="upload-box">
        <h2 className="upload-title">Upload SBOM</h2>

        <div className="upload-type-toggle">
          <button
            className={uploadType === "archive" ? "active" : ""}
            onClick={() => setUploadType("archive")}
          >
            Upload Source Folder
          </button>
          <button
            className={uploadType === "dependency" ? "active" : ""}
            onClick={() => setUploadType("dependency")}
          >
            Upload Dependency File
          </button>
        </div>

        {/* File Input */}
        <input
          type="file"
          ref={fileInputRef}
          style={{ display: "block" }}
          accept={uploadType === "archive" ? ".zip,.tar,.tar.gz,.tgz" : ".json,.xml,.txt"}
          onChange={(e) => setFile(e.target.files?.[0] || null)}
          className="text-input"
          key={uploadType} // Important to reset input when type changes
        />
        <p style={{ fontSize: "0.8rem", color: "gray" }}>
          {uploadType === "archive"
            ? "Accepted formats: .zip, .tar, .tar.gz, .tgz"
            : "Accepted formats: .json, .xml, .txt"}
        </p>

        {/* Common Fields */}
        <select
          className="dropdown"
          value={category}
          onChange={(e) => setCategory(e.target.value)}
        >
          <option value="" disabled hidden>
            Select Category
          </option>
          <option value="Fitness Wearables">Fitness Wearables</option>
          <option value="Smart Home">Smart Home</option>
        </select>

        <input
          type="text"
          placeholder="Enter Device Name"
          value={deviceName}
          onChange={(e) => setDeviceName(e.target.value)}
          className="text-input"
          required
        />

        <input
          type="text"
          placeholder="Manufacturer"
          className="text-input"
          value={manufacturer}
          onChange={(e) => setManufacturer(e.target.value)}
        />

        <input
          type="text"
          placeholder="Operating System"
          className="text-input"
          value={operatingSystem}
          onChange={(e) => setOperatingSystem(e.target.value)}
        />
        <input
          type="text"
          placeholder="OS Version"
          className="text-input"
          value={osVersion}
          onChange={(e) => setOsVersion(e.target.value)}
        />
        <input
          type="text"
          placeholder="Kernel Version"
          className="text-input"
          value={kernelVersion}
          onChange={(e) => setKernelVersion(e.target.value)}
        />

        <button
          className="upload-button"
          onClick={handleUpload}
          disabled={
            loading || !file || !category || !deviceName
          }
        >
          {loading ? "Uploading..." : "Upload"}
        </button>

        {error && <p className="error-message">{error}</p>}
      </div>
    </div>
  );
}
