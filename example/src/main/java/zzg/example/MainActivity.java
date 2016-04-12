/*
 * Copyright 2016 曾志刚
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zzg.example;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import zzg.klinechart.KLineChart;
import zzg.klinechart.internal.Entry;
import zzg.klinechart.internal.EntryData;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);


    //----------------------------
    final EntryData data = new EntryData();
    for (int i = 0; i < 567; i++) {
      float mult = 100;
      float val = (float) (Math.random() * 40) + mult;

      float high = (float) (Math.random() * 9) + 8f;
      float low = (float) (Math.random() * 9) + 8f;

      float open = (float) (Math.random() * 6) + 1f;
      float close = (float) (Math.random() * 6) + 1f;

      boolean even = i % 2 == 0;

      data.addEntry(new Entry(
          val + high,
          val - low,
          even ? val + open : val - open,
          even ? val - close : val + close,
          (int) (Math.random() * 111),
          ""));
    }

    final KLineChart chart = (KLineChart) findViewById(R.id.chart);
    chart.setData(data);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        data.entries.removeLast();
        data.entries.removeLast();
        data.entries.removeLast();
        data.entries.removeLast();
        data.entries.removeLast();
        data.entries.removeLast();
        data.entries.removeLast();
        data.entries.removeLast();

        chart.notifyDataSetChanged(true);
      }
    });
  }
}
