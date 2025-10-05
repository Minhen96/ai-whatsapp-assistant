/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./public/index.html",
    "./src/**/*.{js,jsx,ts,tsx}"
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: '#0a84ff',
          dark: '#0071e3'
        }
      },
      boxShadow: {
        soft: '0 10px 30px rgba(0,0,0,0.08)'
      },
      borderRadius: {
        xl: '1rem'
      }
    }
  },
  plugins: []
}


