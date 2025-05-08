"use client";
// export const dynamic = 'force-dynamic';

import { Suspense } from "react";
import DeviceDetailsContent from "./DeviceDetailsContent";

export default function DeviceDetailsPage() {
  return (
    <Suspense fallback={<div>Loading device page...</div>}>
      <DeviceDetailsContent />
    </Suspense>
  );
}
