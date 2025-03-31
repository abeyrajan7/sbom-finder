"use client";

import React, { useState } from "react";
import "./upload.css";

export default function UploadSBOMPage() {
  const [file, setFile] = useState<File | null>(null);
  const [category, setCategory] = useState("Fitness Wearables");

  const handleUpload = async () => {
    if (!file) {
      alert("Please select a file");
      return;
    }

    const formData = new FormData();
    formData.append("sbomFile", file);
    formData.append("category", category);

    try {
      const response = await fetch(
        "http://localhost:8080/api/sboms/upload-sbom",
        {
          method: "POST",
          body: formData,
        }
      );

      if (response.ok) {
        const result = await response.text();
        alert(`Success: ${result}`);
      } else {
        alert("Upload failed.");
      }
    } catch (error) {
      console.error("Upload error:", error);
      alert("Upload error occurred");
    }
  };

  return (
    <div className="upload-container">
      <div className="upload-box">
        <h2 className="upload-title">Upload SBOM File</h2>

        {/* Category Dropdown */}
        <label htmlFor="category">Select Category:</label>
        <select
          id="category"
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          className="dropdown"
        >
          <option value="Fitness Wearables">Fitness Wearables</option>
          <option value="Smart Home">Smart Home</option>
        </select>

        {/* File Input */}
        <input
          type="file"
          className="file-input"
          onChange={(e) => setFile(e.target.files?.[0] || null)}
        />

        {/* Upload Button */}
        <button className="upload-button" onClick={handleUpload}>
          Upload
        </button>

        {file && <p className="file-name">Selected: {file.name}</p>}
      </div>
    </div>
  );
}
