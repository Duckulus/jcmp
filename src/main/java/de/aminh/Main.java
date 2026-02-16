package de.aminh;

import de.aminh.data.tpch.DataLoader;
import de.aminh.data.tpch.TPCHData;

public class Main {
  static void main() {
    TPCHData data = DataLoader.loadData();
  }
}
