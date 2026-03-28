import { ResponsiveContainer, LineChart, Line, YAxis } from 'recharts';

export function MiniChart({ data, isUp, className = '' }) {
  return (
    <div className={className}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data}>
          <YAxis hide domain={['auto', 'auto']} />
          <Line
            type="monotone"
            dataKey="v"
            stroke={isUp ? 'var(--color-brand-red)' : 'var(--color-brand-blue)'}
            strokeWidth={2}
            dot={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
