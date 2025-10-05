import React from 'react';
import { MODE_CONFIG } from '../../utils/constants';

const ModeIcon = ({ mode }) => {
  const config = MODE_CONFIG[mode];
  return <span className="text-lg">{config?.icon || '🤖'}</span>;
};

export default ModeIcon;