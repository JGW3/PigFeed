# 🐷 Pig Feed App

A comprehensive JavaFX application for managing pig feed nutrition calculations and cost tracking. Perfect for pig farmers who want to optimize feed costs while maintaining proper nutrition.

## ✨ Features

### 🏠 **Welcome Dashboard**
- Clean, modern tabbed interface
- Instant startup with background loading
- Easy navigation between all features

### 🧮 **Feed Mix Calculator**
- Create and save custom feed mixes
- Calculate nutrition profiles (protein, fat, fiber, lysine)
- Price optimization algorithm to minimize costs
- Add new ingredients or edit existing ones
- Save and load feed formulations

### 💰 **Cost Tracker**
- Track all pig-related expenses by category
- **Categories**: Feed, Supplements, Veterinary, Equipment, Maintenance, Other
- Price per unit tracking (lb, 50lbs, 100lbs, ton)
- **Dual cost summaries**: 
  - **Costs YTD** (current year)
  - **Costs Since Starting** (all time)

### 📊 **Spending Reports**
- **Visual Charts**: Toggle between pie charts and line charts
- **Time Periods**: Monthly, Yearly, or All Time views
- **Category Breakdown**: Click any category to drill down into monthly details
- **Interactive**: Click "Feed" to see monthly feed spending, etc.

## 🚀 Getting Started

### Prerequisites
- **Java 17** or higher
- **Maven** for building

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/JGW3/PigFeed.git
   cd PigFeed
   ```

2. Build and run:
   ```bash
   mvn compile exec:java -Dexec.mainClass="org.pigfeed.pigfeedapp.Main"
   ```

### Alternative Run Method
You can also run via the Launcher class:
```bash
mvn compile exec:java -Dexec.mainClass="org.pigfeed.pigfeedapp.Launcher"
```

## 🎯 How to Use

### **Cost Tracking**
1. Go to **Cost Tracker** tab
2. Select expense type (Feed, Equipment, etc.)
3. Enter description, cost, and quantity
4. View your spending broken down by **YTD** and **All Time**

### **Feed Mix Calculation**
1. Go to **Feed Mix Calculator** tab  
2. Add ingredients with their nutrition values
3. Set target weights for your mix
4. Use **"Optimize by Nutrition"** to get balanced formulations
5. Save your recipes for future use

### **Spending Analysis**
1. Go to **Cost Tracker** → **Spending Reports** tab
2. Choose time period (Monthly/Yearly/All Time)
3. Select chart type (Pie Chart/Line Chart)
4. **Click any category** in the table to see detailed breakdown
5. Export data (coming soon!)

## 🛠️ Technology Stack

- **JavaFX 17** - Modern UI framework
- **SQLite** - Local database storage
- **Apache POI** - Excel export capability (in progress)
- **Maven** - Build and dependency management

## 📋 TODO & Roadmap

### 🔜 **Coming Soon**
- [ ] **Excel Export** - Export spending reports to .xlsx files
- [ ] **Print Functions** - Print reports and feed formulations
- [ ] **Online Database** - Cloud sync for multi-device access
- [ ] **Flutter Companion App** - Mobile app for field data entry and monitoring

### 💡 **Future Ideas**
- Feed cost forecasting
- Integration with livestock management systems
- Automated feed ordering based on inventory
- Real-time feed consumption tracking

## 🤝 Contributing

This is a personal farm management tool, but suggestions and improvements are welcome! Feel free to:
- Open issues for bugs or feature requests
- Submit pull requests for improvements
- Share your farm management ideas

## 📄 License

This project is open source. Use it freely for your pig farming operations!

## 🙋‍♂️ Support

Questions or need help? This app was built to solve real pig farming challenges. If you have suggestions or run into issues, feel free to reach out via GitHub issues.

---

**Built for pig farmers, by pig farmers** 🐷🌾
