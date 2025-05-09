import React, { useState, useEffect } from "react";
import "./SearchFilterBar.css";

type Props = {
  onSearch: (params: {
    query?: string;
    manufacturer?: string;
    operatingSystem?: string;
  }) => void;
  onReset: () => void;
};

type AnalyticsItem = {
  name: string;
  sboms: number;
};

const SearchFilterBar: React.FC<Props> = ({ onSearch, onReset }) => {
  //     const BASE_URL = 'https://sbom-finder-backend.onrender.com';
  const BASE_URL =
    process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
  const [query, setQuery] = useState("");
  const [manufacturer, setManufacturer] = useState("");
  const [operatingSystem, setOperatingSystem] = useState("");
  const [manufacturersList, setManufacturersList] = useState<string[]>([]);
  const [osList, setOsList] = useState<string[]>([]);

  useEffect(() => {
    fetch(`${BASE_URL}/api/analytics/manufacturers`)
      .then((res) => res.json())
      .then((data: AnalyticsItem[]) => {
        const names = data.map((item) => item.name);
        if (!names.includes("Unknown Manufacturer"))
          names.push("Unknown Manufacturer");
        setManufacturersList(names);
      });

    fetch(`${BASE_URL}/api/analytics/operating-systems`)
      .then((res) => res.json())
      .then((data: AnalyticsItem[]) => {
        const names = data.map((item) => item.name);
        if (!names.includes("Unknown OS")) names.push("Unknown OS");
        setOsList(names);
      });

    // Reset filters when tab becomes visible again
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        handleReset(); // reuse your existing function
      }
    };

    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, []);

  const handleSubmit = () => {
    onSearch({
      query: query || "",
      manufacturer: manufacturer || "",
      operatingSystem: operatingSystem || "",
    });
  };

  const handleReset = () => {
    setQuery("");
    setManufacturer("");
    setOperatingSystem("");
    onReset(); // Calls parent-provided function to reload all devices
  };

  return (
    <div className="search-filter-container">
      <input
        type="text"
        placeholder="Search by name, OS, or kernel..."
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        className="search-input"
      />

      <select
        value={manufacturer}
        onChange={(e) => setManufacturer(e.target.value)}
        className="search-select"
      >
        <option value="">All Manufacturers</option>
        {manufacturersList.map((manu, index) => (
          <option key={index} value={manu}>
            {manu}
          </option>
        ))}
      </select>

      <select
        value={operatingSystem}
        onChange={(e) => setOperatingSystem(e.target.value)}
        className="search-select"
      >
        <option value="">All Operating Systems</option>
        {osList.map((os, index) => (
          <option key={index} value={os}>
            {os}
          </option>
        ))}
      </select>

      <button className="search-button" onClick={handleSubmit}>
        Search
      </button>

      <button className="search-button" onClick={handleReset}>
        Reset
      </button>
    </div>
  );
};

export default SearchFilterBar;
