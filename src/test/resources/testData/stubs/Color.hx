package com.example;

// Enum with field and constructor values: FQN = com.example.Color
enum Color {
  Red;
  Green;
  Blue;
  Rgb(r:Int, g:Int, b:Int);
  Named(name:String);
}
