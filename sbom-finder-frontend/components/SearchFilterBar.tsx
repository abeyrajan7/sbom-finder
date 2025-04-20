'use client';
import React, { useState } from 'react';
import './SearchFilterBar.css';

type Props = {
  onSearch: (params: {
    query?: string;
    manufacturer?: string;
    operatingSystem?: string;
  }) => void;
};

const SearchFilterBar: React.FC<Props> = ({ onSearch }) => {
  const [query, setQuery] = useState('');
  const [manufacturer, setManufacturer] = useState('');
  const [operatingSystem, setOperatingSystem] = useState('');

  const handleSubmit = () => {
    onSearch({
      query: query || '',
      manufacturer: manufacturer || '',
      operatingSystem: operatingSystem || '',
    });
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
        <option value="Fitbit">Fitbit</option>
        <option value="Apple">Apple</option>
        <option value="Samsung">Samsung</option>
        <option value="Google / AOSP">Google / AOSP</option>
      </select>
      <select
        value={operatingSystem}
        onChange={(e) => setOperatingSystem(e.target.value)}
        className="search-select"
      >
        <option value="">All Operating Systems</option>
        <option value="Wear OS 4">Wear OS 4</option>
        <option value="watchOS 10.3">watchOS 10.3</option>
        <option value="Fitbit OS 5.2">Fitbit OS 5.2</option>
        <option value="Android 15">Android 15</option>
      </select>
      <button className="search-button" onClick={handleSubmit}>
        Search
      </button>
    </div>
  );
};

export default SearchFilterBar;
